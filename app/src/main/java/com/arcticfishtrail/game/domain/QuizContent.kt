package com.arcticfishtrail.game.domain

data class QuizQuestion(val text: String, val options: List<String>, val correctIndex: Int)

/**
 * A quiz set. In this game the three sets are difficulty levels (Easy / Medium / Hard).
 * [iconItem] is the index of the `item_XX` icon shown for the set in the UI.
 */
data class QuizCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconItem: Int,
    val questions: List<QuizQuestion>,
)

/**
 * ============================================================================================
 *  QUIZ CONTENT — theme: fishing (ice fishing first). REPLACEABLE.
 *  3 difficulty levels × 10 single-answer questions, 4 options each, exactly one correct.
 *  Edit texts freely; keep `correctIndex` (0-based) pointing at the right option and keep the
 *  ids stable — best scores are stored per id.
 * ============================================================================================
 */
object QuizContent {

    val categories: List<QuizCategory> = listOf(
        QuizCategory(
            id = "easy",
            title = "Easy",
            subtitle = "First steps on the ice",
            iconItem = 2, // bobber
            questions = listOf(
                QuizQuestion("What do anglers use to cast a line and catch fish?", listOf("A paintbrush", "A fishing rod", "A hammer", "An umbrella"), 1),
                QuizQuestion("The red-and-white float that bobs when a fish bites is called a…", listOf("Bobber", "Sinker", "Reel", "Hook"), 0),
                QuizQuestion("Which tool drills a hole through thick ice?", listOf("A whisk", "A paddle", "An ice auger", "A spatula"), 2),
                QuizQuestion("Where do anglers keep hooks, lures and spare line?", listOf("In a bread box", "In a mailbox", "In a flower pot", "In a tackle box"), 3),
                QuizQuestion("Which part of a fishing rod winds the line back in?", listOf("The reel", "The tip", "The handle cap", "The line guide"), 0),
                QuizQuestion("What do you put on a hook to attract fish?", listOf("Glue", "Bait", "Sand", "Salt"), 1),
                QuizQuestion("What keeps an angler's hands warm on the ice?", listOf("Sunglasses", "Flip-flops", "Mittens", "A swimsuit"), 2),
                QuizQuestion("How do fish breathe underwater?", listOf("Through their fins", "Through their tail", "They hold their breath", "With gills"), 3),
                QuizQuestion("Which net helps lift a hooked fish out of the water?", listOf("A landing net", "A tennis net", "A hairnet", "A volleyball net"), 0),
                QuizQuestion("What is ice fishing?", listOf("Catching fish frozen in ice", "Fishing through a hole cut in the ice", "Fishing from an iceberg", "Keeping caught fish on ice"), 1),
            ),
        ),
        QuizCategory(
            id = "medium",
            title = "Medium",
            subtitle = "Tackle, tricks and ice safety",
            iconItem = 3, // spoon lure
            questions = listOf(
                QuizQuestion("An artificial bait that imitates a small fish or insect is a…", listOf("Swivel", "Leader", "Lure", "Sinker"), 2),
                QuizQuestion("A curved metal lure that wobbles and flashes when pulled is called a…", listOf("Spoon", "Fork", "Plate", "Cup"), 0),
                QuizQuestion("What is the small weight that takes the bait down deeper?", listOf("Float", "Sinker", "Spool", "Snap"), 1),
                QuizQuestion("What does a fish finder (sonar) mainly show?", listOf("The weather forecast", "Road directions", "The time of sunset", "Fish and depth below you"), 3),
                QuizQuestion("What does \"catch and release\" mean?", listOf("Letting the fish go back alive", "Selling your catch", "Catching two fish at once", "Releasing bait into the water"), 0),
                QuizQuestion("Which knot is a classic for tying line to a hook?", listOf("Bow knot", "Granny knot", "Improved clinch knot", "Shoelace knot"), 2),
                QuizQuestion("What do ice anglers use to scoop slush out of the hole?", listOf("A rake", "An ice skimmer", "A broom", "A trowel"), 1),
                QuizQuestion("A common guideline for the minimum clear ice to walk on is about…", listOf("1 cm (0.5 in)", "3 cm (1 in)", "5 cm (2 in)", "10 cm (4 in)"), 3),
                QuizQuestion("Which toothy predator is a favourite target of ice anglers?", listOf("Northern pike", "Goldfish", "Guppy", "Clownfish"), 0),
                QuizQuestion("In ice fishing, a \"tip-up\" is…", listOf("A type of ice tent", "A flag device that signals a bite", "A heated sled", "A kind of fish soup"), 1),
            ),
        ),
        QuizCategory(
            id = "hard",
            title = "Hard",
            subtitle = "For true trail masters",
            iconItem = 4, // ice auger
            questions = listOf(
                QuizQuestion("Which northern river fish is famous for its tall, sail-like dorsal fin?", listOf("Carp", "Tarpon", "Arctic grayling", "Catfish"), 2),
                QuizQuestion("Which fish lives farther north than any other freshwater fish?", listOf("Arctic char", "Rainbow trout", "Common carp", "Largemouth bass"), 0),
                QuizQuestion("On a line spool, \"pound test\" describes the line's…", listOf("Diameter", "Length", "Colour", "Breaking strength"), 3),
                QuizQuestion("Which line is hardest for fish to see because it bends light almost like water?", listOf("Braided line", "Fluorocarbon", "Steel wire", "Cotton line"), 1),
                QuizQuestion("In tackle, a \"leader\" is…", listOf("The biggest fish in a school", "The angler who catches the most", "A length of line between main line and lure", "The rod's first guide"), 2),
                QuizQuestion("What is the barb on a fishing hook for?", listOf("Keeping the fish from slipping off", "Holding the knot", "Making the hook float", "Attracting fish with shine"), 0),
                QuizQuestion("The burbot is the only freshwater member of which fish family?", listOf("Salmon", "Perch", "Pike", "Cod"), 3),
                QuizQuestion("Anadromous fish are fish that…", listOf("Never leave one lake", "Live in the sea and spawn in fresh water", "Only feed at night", "Live under sea ice all year"), 1),
                QuizQuestion("\"Jigging\" means…", listOf("Trolling behind a moving boat", "Leaving bait on the bottom", "Twitching the lure up and down with the rod", "Casting as far as possible"), 2),
                QuizQuestion("Which organ lets most bony fish control their buoyancy?", listOf("Swim bladder", "Gills", "Lateral line", "Liver"), 0),
            ),
        ),
    )

    fun category(id: String?): QuizCategory? = categories.firstOrNull { it.id == id }
}
