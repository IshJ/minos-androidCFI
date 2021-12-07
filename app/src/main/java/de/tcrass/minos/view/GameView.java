package de.tcrass.minos.view;



import android.content.Context;
import android.graphics.Canvas;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import de.tcrass.minos.R;
import de.tcrass.minos.controller.GameController;
import de.tcrass.minos.model.Game;
import de.tcrass.minos.view.render.GameRenderer;

public class GameView extends View {

    private Game game;
    private GameController gameConroller; 
	private GameRenderer gameRenderer;

	private boolean inBackground = false;
	
	public GameView(Context context, Game game) {
		super(context);
		this.game = game;
		this.gameConroller = gameConroller;
		this.gameRenderer = new GameRenderer(context, game);
		
		setLayoutParams(new FrameLayout.LayoutParams(game.getMetrics()
                .getWidth(), game.getMetrics().getHeight(), Gravity.CENTER));
		
		setBackgroundColor(context.getResources().getColor(R.color.game_background));
	
	}

	@Override
	public void onDraw(Canvas canvas) {
	    super.onDraw(canvas);
	    gameRenderer.render(canvas);
	    
	    if (inBackground) {
	        gameRenderer.renderOverlay(canvas);
	    }
	    
	}

	
	public void setInBackground(boolean inBackground) {
	    this.inBackground = inBackground;
	    this.invalidate();
	}
	
	public boolean isInBackground() {
	    return inBackground;
	}
	
    /* --- Touch de.tcrass.minos.event handling ------------------------------------ */
    
	public void setGameController(GameController gameConroller) {
	    this.gameConroller = gameConroller;
	}
	
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (gameConroller != null) {
            gameConroller.handleDrag(this, ev);
        }
        return true;
    }
 	
}
