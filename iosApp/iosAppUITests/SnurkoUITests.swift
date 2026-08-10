import XCTest

private let waitTimeout: TimeInterval = 30

final class SnurkoUITests: XCTestCase {

    override func setUp() {
        continueAfterFailure = false
    }

    /// Walks a fresh install from the empty setup sheet to the home page and back out of a
    /// pushed screen with the edge swipe, shooting every stop on the way.
    func testWritingAChallengeAndWalkingTheApp() {
        let app = XCUIApplication()
        app.launch()

        expect(app.staticTexts["My challenge"], "the setup title")
        attach(app, named: "05-setup-empty")

        habitField(of: app).tap()
        expect(app.keyboards.element, "the keyboard")
        app.typeText("Workout")
        attach(app, named: "06-setup-keyboard")

        tap(app.buttons["Add habit"], "the add-habit button")
        expect(app.staticTexts["Workout"], "the written habit")

        tap(app.buttons["Start the 90 days"], "the start button")
        tap(app.buttons["Start day 1"], "the confirmation button")

        expect(app.staticTexts["Today"], "the today heading")
        attach(app, named: "07-home-day-one")

        tap(app.buttons["reminders"], "the reminders margin note")
        expect(app.staticTexts["Reminders"], "the reminders title")
        attach(app, named: "08-reminders")

        tap(app.buttons["Set a reminder for Workout"], "the reminder row")
        expect(app.datePickers.element, "the time picker sheet")
        attach(app, named: "09-time-picker")

        tap(app.buttons["Cancel"], "the picker cancel button")
        XCTAssertTrue(
            app.datePickers.element.waitForNonExistence(timeout: waitTimeout),
            "the time picker sheet stayed up"
        )

        swipeBackFromTheStartEdge(of: app)
        expect(app.staticTexts["Today"], "the home page after the back swipe")
        attach(app, named: "10-home-after-back-swipe")
    }

    private func habitField(of app: XCUIApplication) -> XCUICoordinate {
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.63))
    }

    private func swipeBackFromTheStartEdge(of app: XCUIApplication) {
        let edge = app.coordinate(withNormalizedOffset: CGVector(dx: 0.0, dy: 0.5))
        let across = app.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5))
        edge.press(forDuration: 0.1, thenDragTo: across)
    }

    private func expect(_ element: XCUIElement, _ what: String) {
        XCTAssertTrue(element.waitForExistence(timeout: waitTimeout), "never saw \(what)")
    }

    private func tap(_ element: XCUIElement, _ what: String) {
        expect(element, what)
        element.tap()
    }

    private func attach(_ app: XCUIApplication, named name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "\(name).png"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
