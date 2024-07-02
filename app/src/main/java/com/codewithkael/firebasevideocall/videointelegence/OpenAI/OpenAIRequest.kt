import com.google.gson.annotations.SerializedName

data class OpenAIRequest(
    val model: String,
    val prompt: String,
    val max_tokens: Int,
    val temperature: Float,
    val top_k: Int,
    val top_p: Float,
    @SerializedName("image") val input_image: String
)

data class OpenAIResponse(
    val id: String,
    @SerializedName("object") val obj: String, // Use a different name and map it to the JSON key "object"
    val created: Long,
    val model: String,
    val choices: List<Choice>
)

data class Choice(
    val text: String,
    val index: Int,
    val logprobs: Logprobs?,
    val finish_reason: String?
)

data class Logprobs(
    val tokens: List<String>,
    val token_logprobs: List<Double>,
    val top_logprobs: List<Map<String, Double>>,
    val text_offset: List<Int>
)
