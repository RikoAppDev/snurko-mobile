package app.snurko.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import app.snurko.core.ios.RootViewControllerProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.LocalTime
import org.koin.compose.koinInject
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIAction
import platform.UIKit.UIAdaptivePresentationControllerDelegateProtocol
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonSystemItem
import platform.UIKit.UIColor
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle
import platform.UIKit.UIPresentationController
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.UIToolbar
import platform.UIKit.UIViewController
import platform.UIKit.presentationController
import platform.UIKit.sheetPresentationController
import platform.UIKit.systemBackgroundColor
import platform.darwin.NSObject

@Composable
actual fun PlatformTimePicker(
    startFrom: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val rootViewControllers = koinInject<RootViewControllerProvider>()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    DisposableEffect(startFrom) {
        val presenter = rootViewControllers.current()
        if (presenter == null) {
            currentOnDismiss()
            return@DisposableEffect onDispose { }
        }

        val sheet = timePickerSheet(
            startFrom = startFrom,
            onCancel = { currentOnDismiss() },
            onConfirm = { hour, minute -> currentOnConfirm(hour, minute) },
        )
        val swipedAway = SwipeDismissObserver { currentOnDismiss() }
        sheet.presentationController?.delegate = swipedAway
        presenter.presentViewController(sheet, animated = true, completion = null)

        onDispose {
            sheet.presentationController?.delegate = null
            swipedAway.stop()
            sheet.dismissViewControllerAnimated(true, completion = null)
        }
    }
}

@Composable
actual fun formattedTime(time: LocalTime): String = remember(time) {
    NSDateFormatter()
        .apply {
            dateStyle = NSDateFormatterNoStyle
            timeStyle = NSDateFormatterShortStyle
        }
        .stringFromDate(time.toNSDate())
}

private class SwipeDismissObserver(
    private val onDismissed: () -> Unit,
) : NSObject(), UIAdaptivePresentationControllerDelegateProtocol {

    private var isListening = true

    fun stop() {
        isListening = false
    }

    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) {
        if (isListening) onDismissed()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun timePickerSheet(
    startFrom: LocalTime,
    onCancel: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
): UIViewController {
    val picker = UIDatePicker(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
        datePickerMode = UIDatePickerMode.UIDatePickerModeTime
        preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
        date = startFrom.toNSDate()
        translatesAutoresizingMaskIntoConstraints = false
    }
    val toolbar = UIToolbar(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
        translatesAutoresizingMaskIntoConstraints = false
        setItems(
            listOf(
                UIBarButtonItem(
                    barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemCancel,
                    primaryAction = UIAction.actionWithHandler { onCancel() },
                ),
                UIBarButtonItem(
                    barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace,
                    target = null,
                    action = null,
                ),
                UIBarButtonItem(
                    barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemDone,
                    primaryAction = UIAction.actionWithHandler {
                        val picked = picker.date.toLocalTime()
                        onConfirm(picked.hour, picked.minute)
                    },
                ),
            ),
            animated = false,
        )
    }

    val controller = UIViewController(nibName = null, bundle = null)
    controller.sheetPresentationController?.detents =
        listOf(UISheetPresentationControllerDetent.mediumDetent())

    val root = controller.view
    root.backgroundColor = UIColor.systemBackgroundColor
    root.addSubview(toolbar)
    root.addSubview(picker)
    NSLayoutConstraint.activateConstraints(
        listOf(
            toolbar.leadingAnchor.constraintEqualToAnchor(root.leadingAnchor),
            toolbar.trailingAnchor.constraintEqualToAnchor(root.trailingAnchor),
            toolbar.topAnchor.constraintEqualToAnchor(root.safeAreaLayoutGuide.topAnchor),
            picker.leadingAnchor.constraintEqualToAnchor(root.leadingAnchor),
            picker.trailingAnchor.constraintEqualToAnchor(root.trailingAnchor),
            picker.topAnchor.constraintEqualToAnchor(toolbar.bottomAnchor),
            picker.bottomAnchor.constraintEqualToAnchor(root.safeAreaLayoutGuide.bottomAnchor),
        ),
    )
    return controller
}

private fun LocalTime.toNSDate(): NSDate =
    NSCalendar.currentCalendar.dateBySettingHour(
        hour.toLong(),
        minute.toLong(),
        0,
        NSDate(),
        0uL,
    ) ?: NSDate()

private fun NSDate.toLocalTime(): LocalTime {
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitHour or NSCalendarUnitMinute,
        this,
    )
    return LocalTime(components.hour.toInt(), components.minute.toInt())
}
