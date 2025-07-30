// app/src/main/java/com/example/bprogress/data/MotivationalMessageRepository.kt
package com.example.bprogress.data

data class MotivationalPrompt(val title: String, val message: String)

object MotivationalMessageRepository {

    private val genericPrompts = listOf(
        MotivationalPrompt("Friendly Reminder!", "Hey, it's already time for your: %s!"),
        MotivationalPrompt("Let's Go!", "Don't forget to perform your %s activity today!"),
        MotivationalPrompt("You Got This!", "A little progress each day adds up. Time for your %s."),
        MotivationalPrompt("Stay Consistent!", "Your %s is waiting. Keep up the great work!")
    )

    fun getRandomMessageForActivity(activityName: String): MotivationalPrompt {
        // For now, just using generic prompts. You could expand this.
        val randomPromptTemplate = genericPrompts.random()
        return MotivationalPrompt(
            title = randomPromptTemplate.title,
            message = String.format(randomPromptTemplate.message, activityName)
        )
    }
}
