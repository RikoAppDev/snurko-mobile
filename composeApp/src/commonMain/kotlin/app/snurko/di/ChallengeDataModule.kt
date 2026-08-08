package app.snurko.di

import app.snurko.challenge.data.RoomChallengeRepository
import app.snurko.challenge.domain.ChallengeRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val challengeDataModule: Module = module {
    singleOf(::RoomChallengeRepository) { bind<ChallengeRepository>() }
}
