package org.sosnetwork.app.data

import android.content.Context
import org.sosnetwork.app.data.local.SosDatabase
import org.sosnetwork.app.data.repo.AlertRepository
import org.sosnetwork.app.data.repo.IdentityRepository
import org.sosnetwork.app.data.repo.VerificationRepository
import org.sosnetwork.app.mesh.SosMeshCoordinator

class AppContainer(context: Context) {
    private val db = SosDatabase.get(context)
    val identityRepository = IdentityRepository(context, db.identityDao())
    val verificationRepository = VerificationRepository(context, db.verificationDao())
    val alertRepository = AlertRepository(db.alertDao())
    val meshCoordinator = SosMeshCoordinator(
        context, identityRepository, alertRepository, verificationRepository
    )
}
