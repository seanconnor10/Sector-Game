package com.disector.editor;

import com.badlogic.gdx.files.FileHandle;
import com.disector.AppFocusTarget;
import com.disector.inputrecorder.InputChainNode;
import com.disector.maploader.TextFileMapLoader;

class MenuPanel extends Panel {
    public MenuPanel(Editor1 editor) {
        super(editor);
        this.input = new InputChainNode(editor.input, "Menu-Panel-Input");

        Button newMapButton = new Button(editor, this, "NEW");
        newMapButton.releaseAction = (Void) -> {
            editor.app.walls.clear();
            editor.app.sectors.clear();
            editor.shouldUpdateViewRenderer = true;
          return Void;
        };
        buttons.add(newMapButton);

        Button loadButton = new Button(editor, this, "LOAD");
        loadButton.releaseAction = (Void) -> {
            FileHandle file = editor.app.activeMapFile;
            if (file == null)
                return Void;
            editor.loadMap(file.path());
            editor.shouldUpdateViewRenderer = true;
            return Void;
        };
        buttons.add(loadButton);

        Button saveButton = new Button(editor, this, "SAVE");
        saveButton.releaseAction = (Void) -> {
            FileHandle file = editor.app.activeMapFile;
            if (file == null)
                return Void;
            String path = file.path();
            new TextFileMapLoader(editor.app).save(path);
            editor.messageLog.log("Saved to " + path);
            return Void;
        };
        buttons.add(saveButton);

        Button playButton = new Button(editor, this, "PLAY");
        playButton.releaseAction = (Void) -> {
            editor.app.swapFocus(AppFocusTarget.GAME);
            return Void;
        };
        buttons.add(playButton);
    }
}
