/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 *
 * @author kristian
 */
public class MenuStageListener extends ClickListener {
    MenuStage stage;
    
    public MenuStageListener(MenuStage stage) {
        this.stage = stage;
    }     
    
}
