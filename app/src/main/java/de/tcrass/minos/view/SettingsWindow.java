package de.tcrass.minos.view;

import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.greenrobot.eventbus.EventBus;

import de.tcrass.minos.R;
import de.tcrass.minos.model.SettingsFormData;
import de.tcrass.minos.event.GameSettingsChangedEvent;

public class SettingsWindow extends PopupWindow implements OnSeekBarChangeListener {

    private final String TAG = this.getClass().getSimpleName();
    
	private SettingsFormData gameSettings;
	
	private SeekBar mazeSizeSeekBar;

	public SettingsWindow(View content, SettingsFormData gameSettings) {
		super(content);
		Log.i(TAG, "ctor");
		
		this.gameSettings = gameSettings;
		
		Point displaySize = DisplayUtils.getDisplaySize(content.getContext());
		
		content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		setWidth(displaySize.x < displaySize.y ? displaySize.x : displaySize.y);
		setHeight(content.getMeasuredHeight());

		mazeSizeSeekBar = (SeekBar)content.findViewById(R.id.mazeSize);
		mazeSizeSeekBar.setOnSeekBarChangeListener(this);
		mazeSizeSeekBar.setProgress((int)(100 * gameSettings.getMazeSize()));
		
	}


	/* --- OnSeekBarChangeListener implementation --- */
	
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        Log.d(TAG, "onProgressChanged");
        onGameSettingsChanged();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "onStartTrackingTouch");
        onGameSettingsChanged();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "onStopTrackingTouch");
        onGameSettingsChanged();
    }

    /* --- Intent handling ----------------------------------------- */
    
    private void onGameSettingsChanged() {
        Log.i(TAG, "onGameSettingsChanged");
        
        gameSettings.setMazeSize((float)mazeSizeSeekBar.getProgress()/100f);

        EventBus.getDefault().post(new GameSettingsChangedEvent(gameSettings));
    }

}
