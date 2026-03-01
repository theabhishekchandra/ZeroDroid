package com.abhishek.zerodroid.features.ir.domain

data class IrRemoteButton(
    val label: String,
    val icon: String,
    val protocol: IrProtocol,
    val frequency: Int,
    val code: Long
)

data class IrRemoteProfile(
    val brand: String,
    val deviceType: String,
    val buttons: List<IrRemoteButton>
)

object IrRemoteDatabase {
    val profiles: List<IrRemoteProfile> = listOf(
        IrRemoteProfile("Samsung", "TV", listOf(
            IrRemoteButton("Power", "power", IrProtocol.SAMSUNG32, 38000, 0xE0E040BF),
            IrRemoteButton("Vol +", "volume_up", IrProtocol.SAMSUNG32, 38000, 0xE0E0E01F),
            IrRemoteButton("Vol -", "volume_down", IrProtocol.SAMSUNG32, 38000, 0xE0E0D02F),
            IrRemoteButton("Mute", "mute", IrProtocol.SAMSUNG32, 38000, 0xE0E0F00F),
            IrRemoteButton("Ch +", "channel_up", IrProtocol.SAMSUNG32, 38000, 0xE0E048B7),
            IrRemoteButton("Ch -", "channel_down", IrProtocol.SAMSUNG32, 38000, 0xE0E008F7),
            IrRemoteButton("Input", "input", IrProtocol.SAMSUNG32, 38000, 0xE0E0807F),
            IrRemoteButton("Menu", "menu", IrProtocol.SAMSUNG32, 38000, 0xE0E058A7),
            IrRemoteButton("Up", "nav_up", IrProtocol.SAMSUNG32, 38000, 0xE0E006F9),
            IrRemoteButton("Down", "nav_down", IrProtocol.SAMSUNG32, 38000, 0xE0E08679),
            IrRemoteButton("Left", "nav_left", IrProtocol.SAMSUNG32, 38000, 0xE0E0A659),
            IrRemoteButton("Right", "nav_right", IrProtocol.SAMSUNG32, 38000, 0xE0E046B9),
            IrRemoteButton("OK", "nav_ok", IrProtocol.SAMSUNG32, 38000, 0xE0E016E9),
            IrRemoteButton("Back", "back", IrProtocol.SAMSUNG32, 38000, 0xE0E01AE5),
            IrRemoteButton("Exit", "exit", IrProtocol.SAMSUNG32, 38000, 0xE0E0B44B)
        )),
        IrRemoteProfile("LG", "TV", listOf(
            IrRemoteButton("Power", "power", IrProtocol.NEC, 38000, 0x20DF10EF),
            IrRemoteButton("Vol +", "volume_up", IrProtocol.NEC, 38000, 0x20DF40BF),
            IrRemoteButton("Vol -", "volume_down", IrProtocol.NEC, 38000, 0x20DFC03F),
            IrRemoteButton("Mute", "mute", IrProtocol.NEC, 38000, 0x20DF906F),
            IrRemoteButton("Ch +", "channel_up", IrProtocol.NEC, 38000, 0x20DF00FF),
            IrRemoteButton("Ch -", "channel_down", IrProtocol.NEC, 38000, 0x20DF807F),
            IrRemoteButton("Input", "input", IrProtocol.NEC, 38000, 0x20DFD02F),
            IrRemoteButton("Menu", "menu", IrProtocol.NEC, 38000, 0x20DFC23D),
            IrRemoteButton("Up", "nav_up", IrProtocol.NEC, 38000, 0x20DF02FD),
            IrRemoteButton("Down", "nav_down", IrProtocol.NEC, 38000, 0x20DF827D),
            IrRemoteButton("Left", "nav_left", IrProtocol.NEC, 38000, 0x20DFE01F),
            IrRemoteButton("Right", "nav_right", IrProtocol.NEC, 38000, 0x20DF609F),
            IrRemoteButton("OK", "nav_ok", IrProtocol.NEC, 38000, 0x20DF22DD),
            IrRemoteButton("Back", "back", IrProtocol.NEC, 38000, 0x20DF14EB),
            IrRemoteButton("Exit", "exit", IrProtocol.NEC, 38000, 0x20DFDA25)
        )),
        IrRemoteProfile("Sony", "TV", listOf(
            IrRemoteButton("Power", "power", IrProtocol.SONY, 40000, 0xA90),
            IrRemoteButton("Vol +", "volume_up", IrProtocol.SONY, 40000, 0x490),
            IrRemoteButton("Vol -", "volume_down", IrProtocol.SONY, 40000, 0xC90),
            IrRemoteButton("Mute", "mute", IrProtocol.SONY, 40000, 0x290),
            IrRemoteButton("Ch +", "channel_up", IrProtocol.SONY, 40000, 0x090),
            IrRemoteButton("Ch -", "channel_down", IrProtocol.SONY, 40000, 0x890),
            IrRemoteButton("Input", "input", IrProtocol.SONY, 40000, 0xA50),
            IrRemoteButton("Menu", "menu", IrProtocol.SONY, 40000, 0x070),
            IrRemoteButton("Up", "nav_up", IrProtocol.SONY, 40000, 0x2F0),
            IrRemoteButton("Down", "nav_down", IrProtocol.SONY, 40000, 0xAF0),
            IrRemoteButton("Left", "nav_left", IrProtocol.SONY, 40000, 0x2D0),
            IrRemoteButton("Right", "nav_right", IrProtocol.SONY, 40000, 0xCD0),
            IrRemoteButton("OK", "nav_ok", IrProtocol.SONY, 40000, 0xA70),
            IrRemoteButton("Back", "back", IrProtocol.SONY, 40000, 0xC70),
            IrRemoteButton("Exit", "exit", IrProtocol.SONY, 40000, 0xC70)
        ))
    )

    fun getProfile(brand: String, deviceType: String): IrRemoteProfile? =
        profiles.find { it.brand == brand && it.deviceType == deviceType }

    fun getBrands(): List<String> = profiles.map { it.brand }.distinct()
}
