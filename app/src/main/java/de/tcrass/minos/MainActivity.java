/*
    Minos - a little maze game
 
    Copyright (C)2020 Torsten Crass
    
    https://gitlab.com/tcrass/minos
    me@tcrass.de

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */

package de.tcrass.minos;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import de.tcrass.minos.controller.GameController;
import de.tcrass.minos.model.Game;
import de.tcrass.minos.model.GameState;
import de.tcrass.minos.model.SettingsFormData;
import de.tcrass.minos.model.factory.GameFactory;
import de.tcrass.minos.model.factory.GameMetrics;
import de.tcrass.minos.view.DisplayUtils;
import de.tcrass.minos.view.GameView;
import de.tcrass.minos.view.SettingsWindow;
import de.tcrass.minos.event.GameSettingsChangedEvent;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String STATE_KEY_GAME = "game";
    private static final String STATE_KEY_GAME_SETTINGS = "gameSettings";
    private static final String STATE_KEY_SCREEN_ORIENTATION = "screenOrientation";
    public static final String PACKAGE_NAME = "de.tcrass.mino";

    private FrameLayout gameContainer = null;
    private GameView gameView = null;
    private SettingsWindow settingsWindow = null;
    private Menu menu;

    private GameMetrics gameMetrics = null;

    private GameController gameController = null;

    private Animation destroyMazeAnimation = null;
    private Animation createMazeAnimation = null;

    /* --- Instance state --- */
    private Game game;
    private SettingsFormData gameSettings;
    private int screenOrientation;

    /* --- Live cycle methods ------------------------------------- */

    /*
     * Activity has been instantiated
     */
    private static Long timingCount;
    static Lock ground_truth_insert_locker = new ReentrantLock();
    static int waitVal = 1000;
    Map<String, String> configMap = new HashMap<>();
    static final String CONFIG_FILE_PATH = "/data/local/tmp/config.out";
    public static Map<String, Integer> methodIdMap = new HashMap<>();

    public static CacheScan cs = null;

    public static int fd = -2;
    private Messenger mService;

    private Messenger replyMessenger = new Messenger(new MessengerHandler());
    public static ArrayList<SideChannelValue> sideChannelValues = new ArrayList<>();
    public static ArrayList<GroundTruthValue> groundTruthValues = new ArrayList<>();
    public static final List<MethodStat> methodStats = new ArrayList<>();

    private static Context mContext;

    public static String sideChannelDPPath;
    public static String mainAppDPPath;

    static {
        System.loadLibrary("native-lib");
    }

    Bundle savedState;
    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        this.savedState = savedState;
        Log.i(TAG, "onCreate");

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED

        ) {

            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS
                            , Manifest.permission.WRITE_EXTERNAL_STORAGE
                            , Manifest.permission.CAMERA
                            , Manifest.permission.ACCESS_FINE_LOCATION
                            , Manifest.permission.ACCESS_COARSE_LOCATION},
                    10);
        } else {
            setUpandRun(savedState);

        }



    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 10: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    setUpandRun(savedState);
                } else {
                    finish();
                }
            }
        }
    }

    protected void setUpandRun(Bundle savedState) {
        sideChannelDPPath = getDatabasePath("SideScan").toString();
        mainAppDPPath = getDatabasePath("MainApp").toString();
        fd = createAshMem();
        if (fd < 0) {
            Log.d("ashmem ", "not set onCreate " + fd);
        }

        copyOdex();

        configMap = readConfigFile();
//        configMap.entrySet().forEach(e -> Log.d("configMap: ", e.getKey() + " " + e.getValue()));


        initializeDB();
        initializeDBAop();
        Intent begin = new Intent(this, SideChannelJob.class);
        bindService(begin, conn, Context.BIND_AUTO_CREATE);
        startForegroundService(begin);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        setContentView(R.layout.activity_main);
        gameContainer = (FrameLayout) findViewById(R.id.gameContainer);

        // App has been started for the first time
        if (savedState == null) {

            // Determine and lock current screen orientation
            screenOrientation = DisplayUtils.getCurrentScreenOrientation(this);
            setRequestedOrientation(screenOrientation);

            gameSettings = new SettingsFormData();
        }

    }

    private static class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.d("ashmem", "Received information from the server: " + msg.getData().getString("reply"));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            Message msg = Message.obtain(null, 0);
            Bundle bundle = new Bundle();
            if (fd < 0) {
                Log.d("ashmem ", "not set onServiceConnected " + fd);
            }
            setAshMemVal(fd, 4l);
            try {
                ParcelFileDescriptor desc = ParcelFileDescriptor.fromFd(fd);
                bundle.putParcelable("msg", desc);
                msg.setData(bundle);
                msg.replyTo = replyMessenger;      // 2
                mService.send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

    };

    private Map<String, String> readConfigFile() {
        Map<String, String> configMap = new HashMap<>();
        try {
            List<String> configs = Files.lines(Paths.get(CONFIG_FILE_PATH)).collect(Collectors.toList());
            configs.stream().filter(c -> !c.contains("//") && c.contains(":")).forEach(c -> configMap.put(c.split(":")[0].trim(), c.split(":")[1].trim()));

        } catch (IOException e) {
            Log.d(TAG + "#", e.toString());
        }
        return configMap;
    }

    private void copyOdex() {
        try {

            String oatHome = "/sdcard/Documents/oatFolder/oat/arm64/";
            Optional<String> baseOdexLine = Files.lines(Paths.get("/proc/self/maps")).collect(Collectors.toList())
                    .stream().sequential().filter(s -> s.contains(PACKAGE_NAME) && s.contains("base.odex"))
                    .findAny();
            Log.d("odex", Files.lines(Paths.get("/proc/self/maps")).collect(Collectors.joining("\n")));
            if (baseOdexLine.isPresent()) {
                String odexpath = "/data/app/" + baseOdexLine.get().split("/data/app/")[1];
                String vdexpath = "/data/app/" + baseOdexLine.get().split("/data/app/")[1].replace("odex", "vdex");
//                String odexRootPath = "/data/app/"+baseOdexLine.get().split("/data/app/")[1].replace("/oat/arm64/base.odex","*");
                Log.d(TAG + "#", odexpath);
                Log.d(TAG + "#", "cp " + odexpath + " " + oatHome);
                Process p = Runtime.getRuntime().exec("cp " + odexpath + " " + oatHome);
                p.waitFor();
                p = Runtime.getRuntime().exec("cp " + vdexpath + " " + oatHome);
                Log.d(TAG + "#", "cp " + vdexpath + " " + oatHome);

                p.waitFor();
                Log.d(TAG + "#", "odex copied");

            } else {
                Log.d(TAG + "#", "base odex absent");
            }

        } catch (IOException | InterruptedException e) {
            Log.d(TAG + "#", e.toString());
        }
    }

    public static void copyMethodMap() {
        String methodMapString = methodIdMap.entrySet().parallelStream().map(Object::toString).collect(Collectors.joining("|"));
//        log only allows a max of 4000 chars
        if (methodMapString.length() > 4000) {
            int chunkCount = methodMapString.length() / 4000;     // integer division

            for (int i = 0; i <= chunkCount; i++) {
                int max = 4000 * (i + 1);
                if (max >= methodMapString.length()) {
                    Log.d("MethodMap"+i, methodMapString.substring(4000 * i));
                } else {
                    Log.d("MethodMap"+i, methodMapString.substring(4000 * i, max));
                }
            }
        }
        Log.d("MethodCount", String.valueOf(methodIdMap.size()));

    }



    /*
     * Will be called after onCreate, but only if the Activity has been killed
     * since it was last visible
     */
    @Override
    public void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        Log.i(TAG, "onRestoreInstanceState");

        screenOrientation = savedState.getInt(STATE_KEY_SCREEN_ORIENTATION);
        if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            // Determine and lock current screen orientation
            screenOrientation = DisplayUtils.getCurrentScreenOrientation(this);
            setRequestedOrientation(screenOrientation);
        }

        gameSettings = savedState.getParcelable(STATE_KEY_GAME_SETTINGS);

        game = savedState.getParcelable(STATE_KEY_GAME);
        if (game != null) {
            gameContainer.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Creating game metrics & view...");
                    // Re-create game metrics...
                    gameMetrics = new GameMetrics(gameContainer, gameSettings);
                    // ...assign to game...
                    game.setMetrics(gameMetrics);
                    // ...and create game view
                    createGameView();
                }
            });
        }
    }

    /*
     * Instance state has been restored
     */
    @Override
    protected void onPostCreate(Bundle savedState) {
        super.onPostCreate(savedState);
        Log.i(TAG, "onPostCreate");
        // If this is the application's first launch or no game has been
        // persisted...
        if (game == null) {
            // ...request creation of a new game
            createGame();
        }

    }

    /*
     * Called if activity has previously been stopped, i.e. brought into the
     * background
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart");
    }

    /*
     * Activity becomes visible
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    /*
     * Activity becomes interactive
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");

        // Start listening to game settings changes
        EventBus.getDefault().register(this);
    }

    /* --- Activity is active --- */

    /*
     * Activity has stopped interacting
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");

        // Stop listening to game settings changes
        EventBus.getDefault().unregister(this);
    }

    /*
     * May be called before or after onPause (or not at all)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState");
        outState.putParcelable(STATE_KEY_GAME, game);
        outState.putParcelable(STATE_KEY_GAME_SETTINGS, gameSettings);
        outState.putInt(STATE_KEY_SCREEN_ORIENTATION, screenOrientation);
        super.onSaveInstanceState(outState);
    }

    /*
     * Activity has become invisible
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
        // If there is a game settings window...
        if (settingsWindow != null) {
            // ...close and forget
            settingsWindow.dismiss();
            settingsWindow = null;
        }
    }

    /*
     * Activity will be removed from memory?
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,
                "onDestroy: "
                        + Integer.toString(getChangingConfigurations(), 16));
    }

    /* --- Menu / action handling ---------------------------------- */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        this.menu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected: " + item.getTitle());

        switch (item.getItemId()) {
        case R.id.action_newGame:
            createGame();
            return true;
        case R.id.action_settings:
            showSettingsWindow();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMenu() {
        if (settingsWindow != null) {
            menu.findItem(R.id.action_newGame).setEnabled(false);
            menu.findItem(R.id.action_newGame).getIcon().setAlpha(64);
            menu.findItem(R.id.action_settings).setEnabled(false);
            menu.findItem(R.id.action_settings).getIcon().setAlpha(64);
        } else {
            menu.findItem(R.id.action_newGame).setEnabled(true);
            menu.findItem(R.id.action_newGame).getIcon().setAlpha(255);
            menu.findItem(R.id.action_settings).setEnabled(true);
            menu.findItem(R.id.action_settings).getIcon().setAlpha(255);
        }
    }

    /* --- UI de.tcrass.minos.event handlers --------------------------------------- */

    private void showSettingsWindow() {

        copyMethodMap();
        Log.d(TAG + "#", sideChannelDPPath);

        Process p = null;
        try {
            p = Runtime.getRuntime().exec("cp " + sideChannelDPPath + ".db /sdcard/Documents");

//                    p.waitFor();
//                    p = Runtime.getRuntime().exec("cp " + getDatabasePath("MainApp") + ".db /sdcard/Documents");
            p = Runtime.getRuntime().exec("cp " + mainAppDPPath + ".db /sdcard/Documents");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "showSettingsWindow");
        // If not already visible...
        if (settingsWindow == null) {
            // Pause game and remember old game state
            final GameState oldGameState = game.getState();
            gameController.setGameState(GameState.PAUSED);
            // Create settings window
            View content = getLayoutInflater().inflate(
                    R.layout.settings_window, null);
            View anchor = findViewById(R.id.action_settings);
            settingsWindow = new SettingsWindow(content, gameSettings);
            // Wire up settings window's OK button
            Button okButton = (Button) content.findViewById(R.id.okButton);
            okButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Since we're working directly with the surrounding's
                    // instance's
                    // member rather than a final variable, make sure that
                    // there really is a settings window instance.
                    if (settingsWindow != null) {
                        // Close and forget
                        settingsWindow.dismiss();
                        settingsWindow = null;
                        updateMenu();
                        // New game created?
                        gameView.setInBackground(false);
                        if (game.getState() == GameState.READY) {
                            gameController.setGameState(GameState.PLAYING);
                        } else {
                            gameController.setGameState(oldGameState);
                        }
                    }
                }
            });
            // Place overlay over game
            gameView.setInBackground(true);
            // Show the settings window
            settingsWindow.showAsDropDown(anchor);
            // Will disable all action bar items
            updateMenu();
            Log.d(TAG, "Automation_completed");

        }
    }

    /* --- Game handling ------------------------------------------- */

    private void createGame() {
        Log.i(TAG, "createGame");

        // Forget the old game (if any)
        if (gameController != null) {
            gameController.setGameState(GameState.PAUSED);
        }
        game = null;

        // Store current screen orientation
        int currentScreenOrientation = DisplayUtils
                .getCurrentScreenOrientation(this);

        // Only allow screen re-orientation if settings window is not showing
        if (settingsWindow == null) {
            // Grant App a chance to re-orient
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            // Store and lock new screen orientation
            screenOrientation = DisplayUtils.getCurrentScreenOrientation(this);
            setRequestedOrientation(screenOrientation);
        }

        // If screen orientation has not changed...
        if (screenOrientation == currentScreenOrientation) {
            gameContainer.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Creating game...");
                    // Determine game metrics
                    gameMetrics = new GameMetrics(gameContainer, gameSettings);
                    // Create game
                    game = GameFactory.createGame(gameMetrics);
                    // Create game view
                    createGameView();
                    gameController = new GameController(MainActivity.this,
                            game, gameView);
                    gameView.setGameController(gameController);
                    if (settingsWindow == null) {
                        gameController.setGameState(GameState.PLAYING);
                    } else {
                        gameView.setInBackground(true);
                    }
                }
            });

        } else {
            // Game will be created during activity re-creation
        }

    }

    private void createGameView() {
        Log.i(TAG, "createGameView");
        // Remove old game view
        if (gameView != null) {
            final ViewGroup gc = gameContainer;
            final View gv = gameView;
            if (createMazeAnimation != null && !createMazeAnimation.hasEnded()) {
                Log.d(TAG, "stopping createMazeAnimation");
                createMazeAnimation.cancel();
            }
            destroyMazeAnimation = AnimationUtils.loadAnimation(this,
                    R.anim.destroy_maze);
            Log.d(TAG, "starting destroyMazeAnimation");
            destroyMazeAnimation.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    gc.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "destroyMazeAnimation finished, removing old game view");
                            gc.removeView(gv);
                        }
                    });
                }
            });
            gameView.startAnimation(destroyMazeAnimation);
        }
        // Create and add new game view
        gameView = new GameView(this, game);
        // gameView.setVisibility(View.GONE);
        gameContainer.addView(gameView, 0);
        // if (createMazeAnimation != null && !createMazeAnimation.hasEnded()) {
        // Log.d(TAG, "stopping createMazeAnimation");
        // createMazeAnimation.cancel();
        // }
        Log.d(TAG, "starting createMazeAnimation");
        createMazeAnimation = AnimationUtils.loadAnimation(this,
                R.anim.create_maze);
        gameView.startAnimation(createMazeAnimation);
    }

    /* --- Game settings handling --- */

    @Subscribe
    public void onGameSettingsChanged(GameSettingsChangedEvent event) {
        Log.i(TAG, "onGameSettingsChanged");
        // Only create new game if metrics have changed
        GameMetrics newGameMetrics = new GameMetrics(gameContainer,
                gameSettings);
        if (!newGameMetrics.equals(gameMetrics)) {
            createGame();
        }
    }


    /**
     * Method to initialize database
     */
    void initializeDB() {
        // Creating the database file in the app sandbox
        SQLiteDatabase db = getBaseContext().openOrCreateDatabase("MainApp.db",
                MODE_PRIVATE, null);
        Locale locale = new Locale("EN", "SG");
        db.setLocale(locale);
        // Creating the schema of the database
        String sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.GROUND_TRUTH + " (" +
                SideChannelContract.Columns.SYSTEM_TIME + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.LABEL + " TEXT, " +
                SideChannelContract.Columns.COUNT + " INTEGER);";
        db.execSQL(sSQL);
        sSQL = "DELETE FROM " + SideChannelContract.GROUND_TRUTH;
        db.execSQL(sSQL);
        db.close();
    }

    void initializeDBAop() {
        // Creating the database file in the app sandbox
        SQLiteDatabase db = getBaseContext().openOrCreateDatabase("MainApp.db",
                MODE_PRIVATE, null);
        Locale locale = new Locale("EN", "SG");
        db.setLocale(locale);
        // Creating the schema of the database
        String sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.GROUND_TRUTH_AOP + " (" +
                SideChannelContract.Columns.METHOD_ID + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.START_COUNT + " INTEGER, " +
                SideChannelContract.Columns.END_COUNT + " INTEGER);";
        db.execSQL(sSQL);
        sSQL = "DELETE FROM " + SideChannelContract.GROUND_TRUTH_AOP;
        db.execSQL(sSQL);
        Log.d("dbinfo", SideChannelContract.GROUND_TRUTH_AOP + " count: " + getRecordCount(SideChannelContract.GROUND_TRUTH_AOP));
        db.close();
    }

    public long getRecordCount(String tableName) {
        SQLiteDatabase db = getBaseContext().openOrCreateDatabase("MainApp.db",
                MODE_PRIVATE, null);
        long count = DatabaseUtils.queryNumEntries(db, tableName);
        db.close();
        return count;
    }

    public static native int setSharedMap();

    public native void setSharedMapChildTest(int shared_mem_ptr, char[] fileDes);

    public native int createAshMem();

    public static native long readAshMem(int fd);

    public static native void setAshMemVal(int fd, long val);

}
