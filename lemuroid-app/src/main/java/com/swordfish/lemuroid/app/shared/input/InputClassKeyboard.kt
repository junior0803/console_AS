package com.swordfish.lemuroid.app.shared.input

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import com.swordfish.lemuroid.app.shared.settings.GameMenuShortcut

object InputClassKeyboard : InputClass {

    private val INPUT_KEYS = listOf(
        KeyEvent.KEYCODE_Q,
        KeyEvent.KEYCODE_W,
        KeyEvent.KEYCODE_E,
        KeyEvent.KEYCODE_R,
        KeyEvent.KEYCODE_T,
        KeyEvent.KEYCODE_Y,
        KeyEvent.KEYCODE_U,
        KeyEvent.KEYCODE_I,
        KeyEvent.KEYCODE_O,
        KeyEvent.KEYCODE_P,
        KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_S,
        KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_F,
        KeyEvent.KEYCODE_G,
        KeyEvent.KEYCODE_H,
        KeyEvent.KEYCODE_J,
        KeyEvent.KEYCODE_K,
        KeyEvent.KEYCODE_L,
        KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_X,
        KeyEvent.KEYCODE_C,
        KeyEvent.KEYCODE_V,
        KeyEvent.KEYCODE_B,
        KeyEvent.KEYCODE_N,
        KeyEvent.KEYCODE_M,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_ESCAPE,
    )

    private val MINIMAL_SUPPORTED_KEYS = listOf(
        KeyEvent.KEYCODE_Q,
        KeyEvent.KEYCODE_W,
        KeyEvent.KEYCODE_E,
        KeyEvent.KEYCODE_R,
        KeyEvent.KEYCODE_T,
        KeyEvent.KEYCODE_Y,
        KeyEvent.KEYCODE_U,
        KeyEvent.KEYCODE_I,
        KeyEvent.KEYCODE_O,
        KeyEvent.KEYCODE_P,
        KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_S,
        KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_F,
        KeyEvent.KEYCODE_G,
        KeyEvent.KEYCODE_H,
        KeyEvent.KEYCODE_J,
        KeyEvent.KEYCODE_K,
        KeyEvent.KEYCODE_L,
        KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_X,
        KeyEvent.KEYCODE_C,
        KeyEvent.KEYCODE_V,
        KeyEvent.KEYCODE_B,
        KeyEvent.KEYCODE_N,
        KeyEvent.KEYCODE_M,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_ESCAPE,
    ).toIntArray()

    private val CUSTOMIZABLE_KEYS = listOf(
        KeyEvent.KEYCODE_Q,
        KeyEvent.KEYCODE_W,
        KeyEvent.KEYCODE_E,
        KeyEvent.KEYCODE_R,
        KeyEvent.KEYCODE_T,
        KeyEvent.KEYCODE_Y,
        KeyEvent.KEYCODE_U,
        KeyEvent.KEYCODE_I,
        KeyEvent.KEYCODE_O,
        KeyEvent.KEYCODE_P,
        KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_S,
        KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_F,
        KeyEvent.KEYCODE_G,
        KeyEvent.KEYCODE_H,
        KeyEvent.KEYCODE_J,
        KeyEvent.KEYCODE_K,
        KeyEvent.KEYCODE_L,
        KeyEvent.KEYCODE_Z,
        KeyEvent.KEYCODE_X,
        KeyEvent.KEYCODE_C,
        KeyEvent.KEYCODE_V,
        KeyEvent.KEYCODE_B,
        KeyEvent.KEYCODE_N,
        KeyEvent.KEYCODE_M,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_SHIFT_LEFT,
    )

    private val DEFAULT_BINDINGS = mapOf(
        KeyEvent.KEYCODE_DPAD_UP to KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN to KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT to KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT to KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_W to KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_A to KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_S to KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_D to KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_I to KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_J to KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_K to KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_L to KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_Q to KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_E to KeyEvent.KEYCODE_BUTTON_L2,
        KeyEvent.KEYCODE_U to KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_O to KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_ENTER to KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_SHIFT_LEFT to KeyEvent.KEYCODE_BUTTON_SELECT,
        KeyEvent.KEYCODE_ESCAPE to KeyEvent.KEYCODE_BUTTON_MODE,
    ).withDefault { KeyEvent.KEYCODE_UNKNOWN }

    override fun getInputKeys() = INPUT_KEYS

    override fun getAxesMap() = emptyMap<Int, Int>()

    override fun getDefaultBindings() = DEFAULT_BINDINGS

    override fun getCustomizableKeys(): List<Int> = CUSTOMIZABLE_KEYS

    override fun isEnabledByDefault(appContext: Context): Boolean {
        return !appContext.packageManager.hasSystemFeature("android.hardware.touchscreen")
    }

    override fun getSupportedShortcuts(): List<GameMenuShortcut> = emptyList()

    override fun isSupported(device: InputDevice): Boolean {
        return sequenceOf(
            (device.sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD,
            device.hasKeys(*MINIMAL_SUPPORTED_KEYS).all { it },
            device.isVirtual.not()
        ).all { it }
    }
}
