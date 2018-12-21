package ap.mnemosyne.uiResources

import ap.mnemosyne.resources.Hint
import ap.mnemosyne.resources.Task

data class TaskCard(val task : Task, val hint : Hint?) : Card