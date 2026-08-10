package app.snurko.core.ios

import platform.UIKit.UIViewController

/** Hands whatever wants to present a system sheet the controller Compose is hosted in. */
class RootViewControllerProvider {

    private var controller: UIViewController? = null

    fun attach(controller: UIViewController) {
        this.controller = controller
    }

    fun detach() {
        controller = null
    }

    fun current(): UIViewController? = controller
}
