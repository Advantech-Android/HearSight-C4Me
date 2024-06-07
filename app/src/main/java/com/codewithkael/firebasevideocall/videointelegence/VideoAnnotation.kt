package com.codewithkael.firebasevideocall.videointelegence

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.api.gax.longrunning.OperationFuture
import com.google.cloud.videointelligence.v1.AnnotateVideoProgress
import com.google.cloud.videointelligence.v1.AnnotateVideoRequest
import com.google.cloud.videointelligence.v1.AnnotateVideoResponse
import com.google.cloud.videointelligence.v1.Feature
import com.google.cloud.videointelligence.v1.NormalizedVertex
import com.google.cloud.videointelligence.v1.TextAnnotation
import com.google.cloud.videointelligence.v1.TextFrame
import com.google.cloud.videointelligence.v1.TextSegment
import com.google.cloud.videointelligence.v1.VideoAnnotationResults
import com.google.cloud.videointelligence.v1.VideoIntelligenceServiceClient
import com.google.cloud.videointelligence.v1.VideoSegment
import com.google.protobuf.ByteString.copyFrom
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


const val TAG="==>VideoAnnotation"
@Singleton
class VideoAnnotation {
    var results:VideoAnnotationResults?=null
    /**
     * Detect text in a video.
     *
     * @param filePath the path to the video file to analyze.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(Exception::class)
    fun detectText(data: ByteArray?): VideoAnnotationResults? {
        VideoIntelligenceServiceClient.create().use { client ->
            // Read file

            // Create the request
            val request: AnnotateVideoRequest = AnnotateVideoRequest.newBuilder()
                .setInputContent(copyFrom(data))
                .addFeatures(Feature.TEXT_DETECTION)
                .build()

            // asynchronously perform object tracking on videos
            val future: OperationFuture<AnnotateVideoResponse, AnnotateVideoProgress> =
                client.annotateVideoAsync(request)
            println("Waiting for operation to complete...")
            // The first result is retrieved because a single video was processed.
            val response: AnnotateVideoResponse = future.get(300, TimeUnit.SECONDS)
            results = response.getAnnotationResults(0)

            // Get only the first annotation for demo purposes.
            val annotation: TextAnnotation = results!!.getTextAnnotations(0)
            System.out.println("Text: " + annotation.getText())

            // Get the first text segment.
            val textSegment: TextSegment = annotation.getSegments(0)
            System.out.println("Confidence: " + textSegment.getConfidence())
            // For the text segment display it's time offset
            val videoSegment: VideoSegment = textSegment.getSegment()
            val startTimeOffset: com.google.protobuf.Duration? = videoSegment.getStartTimeOffset()
            val endTimeOffset: com.google.protobuf.Duration? = videoSegment.getEndTimeOffset()
            // Display the offset times in seconds, 1e9 is part of the formula to convert nanos to seconds
            Log.d(TAG, "detectText: ${ startTimeOffset!!.getSeconds() + startTimeOffset.getNanos() / 1e9}")
            Log.d(TAG, "detectText: ${endTimeOffset!!.getSeconds() + endTimeOffset.getNanos() / 1e9}")

            // Show the first result for the first frame in the segment.
            val textFrame: TextFrame = textSegment.getFrames(0)
            val timeOffset: com.google.protobuf.Duration? = textFrame.getTimeOffset()
            Log.d(TAG, "detectText: ${timeOffset!!.getSeconds() + timeOffset.getNanos() / 1e9}")

            // Display the rotated bounding box for where the text is on the frame.
            println("Rotated Bounding Box Vertices:")
            val vertices: List<NormalizedVertex> =
                textFrame.getRotatedBoundingBox().getVerticesList()
            for (normalizedVertex in vertices) {
                Log.d(TAG, "detectText: ${ normalizedVertex.getX()+ normalizedVertex.getY()}")
            }
        }
        return results
    }
}