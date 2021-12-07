package de.tcrass.minos.aspects;

import static de.tcrass.minos.JobMainAppInsertRunnable.insert_locker;
import static de.tcrass.minos.MainActivity.fd;
import static de.tcrass.minos.MainActivity.methodIdMap;
import static de.tcrass.minos.MainActivity.methodStats;
import static de.tcrass.minos.MainActivity.readAshMem;

import android.util.Log;

import de.tcrass.minos.MethodStat;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;


@Aspect
public class AspectLoggingJava {
    static {
        System.loadLibrary("native-lib");
    }


    final String POINTCUT_METHOD_MAIN_ACTIVITY =
            "execution(* de.tcrass.minos.MainActivity.*(..))";

    @Pointcut(POINTCUT_METHOD_MAIN_ACTIVITY)
    public void executeCfgMainActivity() {
    }

    final String POINTCUT_METHOD_GAME_RENDERER =
            "execution(* de.tcrass.minos.view.render.*.*(..))";

    @Pointcut(POINTCUT_METHOD_GAME_RENDERER)
    public void executeCfgGameRenderer() {
    }

    final String POINTCUT_METHOD_VIEW =
            "execution(* de.tcrass.minos.view.*.*(..))";

    @Pointcut(POINTCUT_METHOD_VIEW)
    public void executeCfgView() {
    }

    final String POINTCUT_METHOD_GAME_EVENT =
            "execution(* de.tcrass.minos.event.*.*(..))";

    @Pointcut(POINTCUT_METHOD_GAME_EVENT)
    public void executeCfgGameEvent() {
    }

    final String POINTCUT_METHOD_GAME_CONTROLLER =
            "execution(* de.tcrass.minos.controller.GameController.*(..))";

    @Pointcut(POINTCUT_METHOD_GAME_CONTROLLER)
    public void executeCfgGameController() {
    }

    final String POINTCUT_METHOD_GAME_MODEL_FACTORY =
            "execution(* de.tcrass.minos.model.factory.*.*(..))";

    @Pointcut(POINTCUT_METHOD_GAME_MODEL_FACTORY)
    public void executeCfgModelFactory() {
    }


    final String POINTCUT_METHOD_GAME_MODEL =
            "execution(* de.tcrass.minos.model.*.*(..))";

    @Pointcut(POINTCUT_METHOD_GAME_MODEL)
    public void executeCfgModel() {
    }


    //    @Pointcut("!within(org.woheller69.weather.activities.SplashActivity.*.*(..))")
//    public void notAspectSplashActivity() { }
//
//    @Pointcut("!within(org.woheller69.weather.activities.*.onCreate(..))")
//    public void notAspect() { }
//
//    @Around("executeManageLocationsActivity() || executeSplashRunView() || executeSplashGetRecordCount() " +
//            "|| executeRainViewer() || executeChildA() || executeChildB() || executeChildC()")
    @Around(""
//            + "executeCfgMainActivity() "
            + "executeCfgGameRenderer() "
            + "|| executeCfgGameController() "
            + "|| executeCfgGameEvent() "
            + "|| executeCfgModelFactory() "
            + "|| executeCfgView() "
            + "|| executeCfgModel() "
    )
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            methodIdMap.putIfAbsent(methodSignature.toString(), methodIdMap.size());

            long startFd = fd > 0 ? readAshMem(fd) : -1;
//            long startT = System.currentTimeMillis();

            Object result = joinPoint.proceed();
//            long endT = System.currentTimeMillis();

            long endFd = fd > 0 ? readAshMem(fd) : -1;

            MethodStat methodStat = new MethodStat(methodIdMap.get(methodSignature.toString()), startFd, endFd);
//            Log.d("#Aspect ", methodStat.getId()+" "+startFd+" "+endFd);
            insert_locker.lock();
            if (methodStats.isEmpty()) {
                methodStats.add(methodStat);

            } else if (!methodStats.get(methodStats.size() - 1).equals(methodStat)) {
                methodStats.add(methodStat);
            }
            insert_locker.unlock();

            Log.d("LoggingVM ", methodStat.toString());
//            Log.v("LoggingVM ",
//                    methodSignature.toString()+" "+methodSignature.toLongString()+ methodSignature.toShortString());
            return result;
        } catch (Exception e) {
            return joinPoint.proceed();
        }
    }

//    @Before("executeManageLocationsActivity() || executeSplashRunView() || executeSplashGetRecordCount() " +
//            "|| executeRainViewer() || executeChildA() || executeChildB() || executeChildC()")
//    @Before("executeRainViewer() || executeChildA() || executeChildB() || executeChildC()")
//    @Before("executeCfgChildA() || executeCfgChildB() || executeCfgChildC()")
//    public void weaveJoinPoint(JoinPoint joinPoint) throws Throwable {
//        if (joinPoint == null) {
//            return;
//        }
//        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
//        methodIdMap.putIfAbsent(methodSignature.toString(), methodIdMap.size());
//
//        long startFd = fd > 0 ? readAshMem(fd) : -1;
////        long endFd = fd > 0 ? readAshMem(fd) : -1;
//        long endFd = startFd;
//        MethodStat methodStat = new MethodStat(methodIdMap.get(methodSignature.toString()), startFd, endFd);
////        Log.d("#Aspect ", methodSignature.toString()+" "+startFd+" "+endFd);
//        insert_locker.lock();
//        if (methodStats.isEmpty()) {
//            methodStats.add(methodStat);
//
//        } else if (!methodStats.get(methodStats.size() - 1).equals(methodStat)) {
//            methodStats.add(methodStat);
//        }
//        insert_locker.unlock();
//
//    }

    public static native long readAshMem1(int fd);
}
