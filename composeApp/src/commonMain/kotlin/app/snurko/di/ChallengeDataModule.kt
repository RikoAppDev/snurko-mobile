package app.snurko.di

import app.snurko.challenge.data.DocumentNotebookBackup
import app.snurko.challenge.data.NotebookJsonCodec
import app.snurko.challenge.data.RoomChallengeRepository
import app.snurko.challenge.data.RoomNotebookRepository
import app.snurko.challenge.data.RoomSetupDraftRepository
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.NotebookBackup
import app.snurko.challenge.domain.NotebookRepository
import app.snurko.challenge.domain.SetupDraftRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val challengeDataModule: Module = module {
    singleOf(::RoomChallengeRepository) { bind<ChallengeRepository>() }
    singleOf(::RoomNotebookRepository) { bind<NotebookRepository>() }
    singleOf(::RoomSetupDraftRepository) { bind<SetupDraftRepository>() }
    singleOf(::NotebookJsonCodec)
    singleOf(::DocumentNotebookBackup) { bind<NotebookBackup>() }
}
