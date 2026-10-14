package com.example.vangtichai

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.example.vangtichai.databinding.ActivityMainBinding
import com.example.vangtichai.databinding.ViewNoteRowBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

/**
 * Lets the user type an amount on a keypad and shows which Taka notes make up the change.
 *
 * Android picks one of four layouts (phone/tablet x portrait/landscape) from the resource
 * qualifiers. Rotating recreates the activity with the other layout, so the typed amount and
 * the change list are saved in [onSaveInstanceState] and restored in [onCreate].
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Change table rows, in the same order as [ChangeCalculator.NOTES]. */
    private lateinit var noteRows: List<ViewNoteRowBinding>

    /** Digit keys, indexed by their digit. */
    private lateinit var digitKeys: List<MaterialButton>

    /** Digits typed so far, e.g. "688". Empty until the first key press. */
    private var amount = ""

    /** How many of each note in [ChangeCalculator.NOTES] make up [amount]. */
    private var change = LongArray(ChangeCalculator.NOTES.size)

    private val noteColors by lazy { NOTE_COLORS.map { ContextCompat.getColor(this, it) } }
    private val idleRowColor by lazy { ContextCompat.getColor(this, R.color.note_row_idle) }

    /**
     * Lit row backgrounds: each note's colour, faintly, over the card. They are kept opaque so that a
     * fade between idle and lit goes straight from one to the other, never through a brighter colour.
     */
    private val litRowColors by lazy {
        val card = ContextCompat.getColor(this, R.color.card_surface)
        noteColors.map { ColorUtils.compositeColors(ColorUtils.setAlphaComponent(it, ACTIVE_ROW_ALPHA), card) }
    }
    private val labelColor by lazy { ContextCompat.getColor(this, R.color.header_content_secondary) }
    private val strongTextColor by lazy {
        MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
    }
    private val mutedTextColor by lazy {
        val color = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurfaceVariant)
        ColorUtils.setAlphaComponent(color, MUTED_TEXT_ALPHA)
    }

    /** How lit up each row is on screen (0 = faded back, 1 = lit up), and the animations moving it. */
    private val rowLevels = FloatArray(ChangeCalculator.NOTES.size)
    private val rowAnimators = arrayOfNulls<ValueAnimator>(ChangeCalculator.NOTES.size)

    private val restoreCaption = Runnable { binding.captionText.text = caption() }

    /** While the backspace key is held, deletes a digit every [BACKSPACE_REPEAT_INTERVAL] ms. */
    private val repeatBackspace = object : Runnable {
        override fun run() {
            if (amount.isEmpty()) return
            val key = binding.keyBackspace
            key.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onBackspace()
            key.postDelayed(this, BACKSPACE_REPEAT_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars. The app is always dark, so both bars keep light icons even
        // when the rest of the phone uses a light theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        noteRows = with(binding) { listOf(note500, note100, note50, note20, note10, note5, note2, note1) }
        noteRows.forEachIndexed { i, row ->
            row.noteLabel.text = getString(R.string.note_label, ChangeCalculator.NOTES[i])
        }

        digitKeys = with(binding) { listOf(key0, key1, key2, key3, key4, key5, key6, key7, key8, key9) }
        digitKeys.forEachIndexed { digit, key ->
            key.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onDigit(digit)
            }
        }
        binding.keyClear.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClear()
        }

        // Backspace works like a phone's: a tap deletes one digit, holding it keeps deleting.
        binding.keyBackspace.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onBackspace()
        }
        binding.keyBackspace.setOnLongClickListener {
            it.removeCallbacks(repeatBackspace)
            repeatBackspace.run()
            true
        }
        binding.keyBackspace.setOnTouchListener { key, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                key.removeCallbacks(repeatBackspace)
            }
            false // The button still handles the touch itself: press state, click and long click
        }

        // After a rotation, put back the amount and change list that were on screen.
        if (savedInstanceState != null) {
            amount = savedInstanceState.getString(KEY_AMOUNT).orEmpty()
            change = savedInstanceState.getLongArray(KEY_CHANGE)
                ?: ChangeCalculator.breakdown(ChangeCalculator.valueOf(amount))
        }
        render(animate = false)
        if (savedInstanceState == null) playEntrance()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_AMOUNT, amount)
        outState.putLongArray(KEY_CHANGE, change)
    }

    /** Hardware keyboards work too: digits type, Backspace deletes, Esc or C clears. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val key = when (keyCode) {
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> digitKeys[keyCode - KeyEvent.KEYCODE_0]
            in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> digitKeys[keyCode - KeyEvent.KEYCODE_NUMPAD_0]
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_CLEAR, KeyEvent.KEYCODE_C -> binding.keyClear
            KeyEvent.KEYCODE_DEL -> binding.keyBackspace
            else -> return super.onKeyDown(keyCode, event)
        }
        key.flashPressed()
        key.performClick()
        return true
    }

    private fun onDigit(digit: Int) {
        val updated = ChangeCalculator.appendDigit(amount, digit)
        if (updated == null) {
            rejectDigit()
            return
        }
        setAmount(updated)
    }

    private fun onClear() = setAmount("")

    private fun onBackspace() {
        if (amount.isNotEmpty()) setAmount(ChangeCalculator.removeLastDigit(amount))
    }

    private fun setAmount(newAmount: String) {
        amount = newAmount
        change = ChangeCalculator.breakdown(ChangeCalculator.valueOf(amount))
        render(animate = true)
    }

    /** The amount already has [ChangeCalculator.MAX_DIGITS] digits: shake it and say why. */
    private fun rejectDigit() {
        val amountView = binding.amountText
        amountView.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
            else HapticFeedbackConstants.LONG_PRESS
        )
        val d = resources.getDimension(R.dimen.shake_distance)
        ObjectAnimator.ofFloat(amountView, View.TRANSLATION_X, 0f, d, -d, d / 2, -d / 2, 0f)
            .setDuration(SHAKE_DURATION)
            .start()

        val caption = binding.captionText
        caption.text = resources.getQuantityString(
            R.plurals.caption_max_digits, ChangeCalculator.MAX_DIGITS, ChangeCalculator.MAX_DIGITS
        )
        caption.removeCallbacks(restoreCaption)
        caption.postDelayed(restoreCaption, CAPTION_RESTORE_DELAY)
    }

    private fun render(animate: Boolean) {
        binding.amountText.text = buildSpannedString {
            scale(LABEL_SCALE) { color(labelColor) { append(getString(R.string.label_taka)) } }
            if (amount.isNotEmpty()) {
                append(' ')
                bold { append(amount) }
            }
        }
        binding.amountText.contentDescription =
            if (amount.isEmpty()) getString(R.string.cd_amount_empty) else getString(R.string.cd_amount, amount)

        binding.captionText.removeCallbacks(restoreCaption)
        binding.captionText.text = caption()

        change.forEachIndexed { i, count -> renderRow(i, count, animate) }
        renderBackspace(animate)
        if (animate) binding.amountText.pop(AMOUNT_POP_SCALE)
    }

    /** Like in a phone dialer, the backspace key only shows while there is a digit to delete. */
    private fun renderBackspace(animate: Boolean) {
        val key = binding.keyBackspace
        val show = amount.isNotEmpty()
        val alpha = if (show) 1f else 0f
        if (key.isVisible == show && key.alpha == alpha) return
        key.animate().cancel()
        if (!animate) {
            key.alpha = alpha
            key.isInvisible = !show
            return
        }
        key.isInvisible = false
        key.animate()
            .alpha(alpha)
            .setDuration(BACKSPACE_FADE_DURATION)
            .withEndAction { key.isInvisible = !show }
            .start()
    }

    private fun renderRow(index: Int, count: Long, animate: Boolean) {
        val row = noteRows[index]
        val countText = count.toString()
        val changed = row.noteCount.text.toString() != countText
        row.noteCount.text = countText
        row.root.contentDescription = resources.getQuantityString(
            R.plurals.cd_note_row, count.toInt(), ChangeCalculator.NOTES[index], countText
        )

        // Notes that are part of the change light up in their own colour; the rest fade back.
        lightRow(index, if (count > 0) 1f else 0f, animate)
        if (animate && changed) row.noteCount.pop(COUNT_POP_SCALE, pivotAtEnd = true)
    }

    private fun caption(): String {
        if (amount.isEmpty()) return getString(R.string.hint_enter_amount)
        val notes = change.sum()
        return resources.getQuantityString(R.plurals.caption_note_count, notes.toInt(), notes.toString())
    }

    /** Moves a row towards [target] (0 = faded back, 1 = lit up), smoothly when [animate] is set. */
    private fun lightRow(index: Int, target: Float, animate: Boolean) {
        rowAnimators[index]?.cancel()
        rowAnimators[index] = null
        val shown = rowLevels[index]
        if (!animate || shown == target) {
            setRowLevel(index, target)
            return
        }
        rowAnimators[index] = ValueAnimator.ofFloat(shown, target).apply {
            duration = ROW_FADE_DURATION
            addUpdateListener { setRowLevel(index, it.animatedValue as Float) }
            start()
        }
    }

    /** Colours a row for its [level]: the background, colour tab, note and count all blend together. */
    private fun setRowLevel(index: Int, level: Float) {
        rowLevels[index] = level
        val row = noteRows[index]
        val noteColor = noteColors[index]
        row.noteLabel.setTextColor(ColorUtils.blendARGB(mutedTextColor, noteColor, level))
        row.noteCount.setTextColor(ColorUtils.blendARGB(mutedTextColor, strongTextColor, level))
        row.noteAccent.backgroundTintList = ColorStateList.valueOf(
            ColorUtils.blendARGB(ColorUtils.setAlphaComponent(noteColor, IDLE_ACCENT_ALPHA), noteColor, level)
        )
        row.root.backgroundTintList = ColorStateList.valueOf(ColorUtils.blendARGB(idleRowColor, litRowColors[index], level))
    }

    /**
     * The window is drawn edge to edge, so the header grows under the status bar while the change
     * table and keypad keep clear of the navigation bar and any display cutout.
     */
    private fun applySystemBarInsets() {
        val header = binding.header
        val headerLeft = header.paddingLeft
        val headerTop = header.paddingTop
        val headerRight = header.paddingRight
        val notesMargins = binding.notesCard.layoutParams as ViewGroup.MarginLayoutParams
        val notesStart = notesMargins.marginStart
        val notesBottom = notesMargins.bottomMargin
        val keypadEnd = (binding.keypad.layoutParams as ViewGroup.MarginLayoutParams).marginEnd

        // The root layout is always left-to-right, so start is left and end is right.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            header.updatePadding(
                left = headerLeft + bars.left,
                top = headerTop + bars.top,
                right = headerRight + bars.right,
            )
            binding.notesCard.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = notesStart + bars.left
                bottomMargin = notesBottom + bars.bottom
            }
            // The keypad takes its top and bottom from the change table, so it only needs the side.
            binding.keypad.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd = keypadEnd + bars.right
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    /** On a fresh start the change table, then the keypad, rise gently into place. */
    private fun playEntrance() {
        val rise = resources.getDimension(R.dimen.entrance_rise)
        listOf(binding.notesCard, binding.keypad).forEachIndexed { i, section ->
            section.alpha = 0f
            section.translationY = rise
            section.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(ENTRANCE_DELAY + i * ENTRANCE_STAGGER)
                .setDuration(ENTRANCE_DURATION)
                .setInterpolator(DecelerateInterpolator(ENTRANCE_DECELERATION))
                .start()
        }
    }

    /** Briefly enlarges the view and lets it spring back, drawing the eye to a changed value. */
    private fun View.pop(scale: Float, pivotAtEnd: Boolean = false) {
        animate().cancel()
        pivotX = if (pivotAtEnd) width.toFloat() else width / 2f
        pivotY = height / 2f
        scaleX = scale
        scaleY = scale
        animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(POP_DURATION)
            .setInterpolator(OvershootInterpolator(OVERSHOOT_TENSION))
            .start()
    }

    /** Shows the pressed state for a moment, so hardware key presses are visible on screen. */
    private fun View.flashPressed() {
        isPressed = true
        postDelayed({ isPressed = false }, KEY_FLASH_DURATION)
    }

    private companion object {
        const val KEY_AMOUNT = "amount"
        const val KEY_CHANGE = "change"

        /** Note colours, in the same order as [ChangeCalculator.NOTES]. */
        val NOTE_COLORS = intArrayOf(
            R.color.note_500, R.color.note_100, R.color.note_50, R.color.note_20,
            R.color.note_10, R.color.note_5, R.color.note_2, R.color.note_1,
        )

        /** "Taka:" is drawn at this fraction of the amount's text size. */
        const val LABEL_SCALE = 0.62f

        const val ACTIVE_ROW_ALPHA = 0x29
        const val IDLE_ACCENT_ALPHA = 0x40
        const val MUTED_TEXT_ALPHA = 0x99

        const val AMOUNT_POP_SCALE = 1.06f
        const val COUNT_POP_SCALE = 1.35f
        const val POP_DURATION = 280L
        const val OVERSHOOT_TENSION = 3f
        const val ROW_FADE_DURATION = 220L
        const val SHAKE_DURATION = 400L
        const val CAPTION_RESTORE_DELAY = 1600L
        const val KEY_FLASH_DURATION = 90L
        const val BACKSPACE_REPEAT_INTERVAL = 70L
        const val BACKSPACE_FADE_DURATION = 150L
        const val ENTRANCE_DELAY = 60L
        const val ENTRANCE_STAGGER = 70L
        const val ENTRANCE_DURATION = 380L
        const val ENTRANCE_DECELERATION = 2f
    }
}
