package com.app.idisplaynew.data.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ScheduleCurrentResponse(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("result") val result: ScheduleResult? = null,
    @SerialName("errors") val errors: Map<String, List<String>>? = null
) {
    @Serializable
    data class ScheduleResult(
        @SerialName("scheduleId") val scheduleId: Int? = null,
        @SerialName("layoutId") val layoutId: Int? = null,
        @SerialName("layoutName") val layoutName: String? = null,
        @SerialName("startTime") val startTime: String? = null,
        @SerialName("endTime") val endTime: String? = null,
        @SerialName("priority") val priority: Int? = null,
        @SerialName("layout") val layout: Layout? = null,
        @SerialName("tickers") val tickers: List<Ticker>? = null,
        @SerialName("lastUpdated") val lastUpdated: String? = null
    ) {
        @Serializable
        data class Ticker(
            @SerialName("id") val id: Int? = null,
            @SerialName("text") val text: String? = null,
            @SerialName("speed") val speed: Int? = null,
            @SerialName("fontSize") val fontSize: Int? = null,
            @SerialName("textColor") val textColor: String? = null,
            @SerialName("backgroundColor") val backgroundColor: String? = null,
            @SerialName("position") val position: String? = null,
            @SerialName("height") val height: Int? = null,
            @SerialName("brandImageUrl") val brandImageUrl: String? = null,
            @SerialName("priority") val priority: Int? = null
        )
        @Serializable
        data class Layout(
            @SerialName("layoutId") val layoutId: Int? = null,
            @SerialName("name") val name: String? = null,
            @SerialName("screenWidth") val screenWidth: Int? = null,
            @SerialName("screenHeight") val screenHeight: Int? = null,
            @SerialName("orientation") val orientation: String? = null,
            @SerialName("zones") val zones: List<Zone>? = null
        ) {
            @Serializable
            data class Zone(
                @SerialName("zoneId") val zoneId: Int? = null,
                @SerialName("name") val name: String? = null,
                @SerialName("x") val x: Int? = null,
                @SerialName("y") val y: Int? = null,
                @SerialName("width") val width: Int? = null,
                @SerialName("height") val height: Int? = null,
                @SerialName("zIndex") val zIndex: Int? = null,
                @SerialName("backgroundColor") val backgroundColor: String? = null,
                @SerialName("playlist") val playlist: List<PlaylistItem>? = null
            ) {
                @Serializable
                data class PlaylistItem(
                    @SerialName("mediaId") val mediaId: Int? = null,
                    @SerialName("type") val type: String? = null,
                    @SerialName("url") val url: String? = null,
                    @SerialName("fileName") val fileName: String? = null,
                    @SerialName("originalFileName") val originalFileName: String? = null,
                    @SerialName("duration") val duration: Int? = null,
                    @SerialName("fileSizeBytes") val fileSizeBytes: Long? = null,
                    @SerialName("checksum") val checksum: String? = null,
                    @SerialName("htmlContent") val htmlContent: String? = null,
                    @SerialName("sourceUrl") val sourceUrl: String? = null,
                    @SerialName("refreshInterval") val refreshInterval: Int? = null,
                    @SerialName("transitionEffectId") val transitionEffectId: Int? = null,
                    @SerialName("transitionEffectName") val transitionEffectName: String? = null,
                    @SerialName("transitionEffectCssClass") val transitionEffectCssClass: String? = null,
                    @SerialName("transitionEffectDurationMs") val transitionEffectDurationMs: Int? = null
                )
            }
        }
    }
}
