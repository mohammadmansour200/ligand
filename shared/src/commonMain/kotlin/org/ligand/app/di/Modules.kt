package org.ligand.app.di

import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.ligand.app.core.data.HttpClientFactory
import org.ligand.app.editor.data.cheminformatics.RDKitCheminformaticsDataSource
import org.ligand.app.editor.data.network.KtorPubchemDataSource
import org.ligand.app.editor.domain.CheminformaticsDataSource
import org.ligand.app.editor.domain.PubchemDataSource
import org.ligand.app.editor.presentation.drawing_pane.DrawingPaneViewModel
import org.ligand.app.editor.presentation.molecule_pane.MoleculePaneViewModel
import uk.ac.cam.ch.wwmm.opsin.NameToStructure

val sharedModule = module {
    single { HttpClientFactory.create(OkHttp.create()) }
    single { NameToStructure.getInstance() }
    singleOf(::RDKitCheminformaticsDataSource).bind<CheminformaticsDataSource>()
    singleOf(::KtorPubchemDataSource).bind<PubchemDataSource>()

    viewModelOf(::DrawingPaneViewModel)
    viewModelOf(::MoleculePaneViewModel)
}