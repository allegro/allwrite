package pl.allegro.tech.allwrite

public interface PostprocessingRecipe {
    public fun postprocess(): PostprocessingResult
}

public sealed interface PostprocessingResult {
    public data object Success : PostprocessingResult
    public data class Failure(val errorMessage: String) : PostprocessingResult
}
