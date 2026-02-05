package com.app.idisplaynew.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleCurrentResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("result") val result: ScheduleResult? = null,
    @SerialName("errors") val errors: Map<String, List<String>> = emptyMap()
) {
    @Serializable
    data class ScheduleResult(
        @SerialName("scheduleId") val scheduleId: Int? = null,
        @SerialName("layoutId") val layoutId: Int = 0,
        @SerialName("layoutName") val layoutName: String = "",
        @SerialName("startTime") val startTime: String? = null,
        @SerialName("endTime") val endTime: String? = null,
        @SerialName("priority") val priority: Int = 0,
        @SerialName("layout") val layout: Layout? = null,
        @SerialName("tickers") val tickers: List<Ticker> = emptyList(),
        @SerialName("lastUpdated") val lastUpdated: String? = null
    ) {
        @Serializable
        data class Ticker(
            @SerialName("id") val id: Int = 0,
            @SerialName("text") val text: String = "",
            @SerialName("speed") val speed: Int = 5,
            @SerialName("fontSize") val fontSize: Int = 24,
            @SerialName("textColor") val textColor: String = "#000000",
            @SerialName("backgroundColor") val backgroundColor: String = "#ffffff",
            @SerialName("position") val position: String = "bottom",
            @SerialName("height") val height: Int = 50,
            @SerialName("priority") val priority: Int = 1
        )
        @Serializable
        data class Layout(
            @SerialName("layoutId") val layoutId: Int = 0,
            @SerialName("name") val name: String = "",
            @SerialName("screenWidth") val screenWidth: Int = 1920,
            @SerialName("screenHeight") val screenHeight: Int = 1080,
            @SerialName("orientation") val orientation: String = "landscape",
            @SerialName("zones") val zones: List<Zone> = emptyList()
        ) {
            @Serializable
            data class Zone(
                @SerialName("zoneId") val zoneId: Int = 0,
                @SerialName("name") val name: String = "",
                @SerialName("x") val x: Int = 0,
                @SerialName("y") val y: Int = 0,
                @SerialName("width") val width: Int = 0,
                @SerialName("height") val height: Int = 0,
                @SerialName("zIndex") val zIndex: Int = 0,
                @SerialName("backgroundColor") val backgroundColor: String = "#000000",
                @SerialName("playlist") val playlist: List<PlaylistItem> = emptyList()
            ) {
                @Serializable
                data class PlaylistItem(
                    @SerialName("mediaId") val mediaId: Int = 0,
                    @SerialName("type") val type: String = "video",
                    @SerialName("url") val url: String = "",
                    @SerialName("fileName") val fileName: String = "",
                    @SerialName("duration") val duration: Int = 0,
                    @SerialName("fileSizeBytes") val fileSizeBytes: Long = 0L,
                    @SerialName("checksum") val checksum: String? = null
                )
            }
        }
    }
}
