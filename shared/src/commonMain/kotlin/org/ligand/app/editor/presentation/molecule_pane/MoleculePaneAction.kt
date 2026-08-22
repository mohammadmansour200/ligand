package org.ligand.app.editor.presentation.molecule_pane

import org.ligand.app.editor.domain.Molecule

sealed interface MoleculePaneAction {
    /**
     * Action triggered when a molecule is selected on canvas.
     * If molecule is null conformer state is emptied. If not null Conformer is parsed using Cheminformatics engine. generated conformer is then saved in state
     * @param molecule Selected molecule
     */
    data class OnGenerateConformer(val molecule: Molecule?) : MoleculePaneAction

    /**
     * Action triggered when a molecule is selected on canvas.
     * If molecule is null properties state is emptied. If not null Properties are calculated using Cheminformatics engine. properties are then saved in state
     * @param molecule Selected molecule
     */
    data class OnCalcProperties(val molecule: Molecule?) : MoleculePaneAction
}