package com.disector.console;

import com.disector.Application;
import com.badlogic.gdx.Gdx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class CommandExecutor {

    private final Application app;

    private final Map<String, Method> methods = new HashMap<>();
    private String commandList;
    private String[] commandNames;

    public CommandExecutor(Application app) {
        this.app = app;

        List<Method> commands = new ArrayList<>();
        for (Method m : CommandExecutor.class.getMethods()) {
            if (m.isAnnotationPresent(ConsoleCommand.class))
                commands.add(m);
        }

        commands.sort( //Sort alphabetically by method name
                (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName())
        );

        commandNames = new String[commands.size()];

        int i=0;
        for (Method m : commands) {
            methods.put(m.getName().toLowerCase(), m);
            commandNames[i] = m.getName();
            i++;
        }

        StringBuilder str = new StringBuilder();
        for(Method m : commands) {
            str.append(m.getName());

            for (Parameter p : m.getParameters()) {
                str.append(" (").append(p.getType().getName()).append(")");
            }
            String helpText = m.getAnnotation(ConsoleCommand.class).helpText();
            if (helpText != null && !helpText.isEmpty()) {
                str.append(" - ").append(helpText);
            }
            str.append("\n");
        }
        commandList = str.toString();


    }

    public String execute(String command) {
        String response;

        if (command == null)
            return null;

        if (hasInvalidWhitespace(command)) {
            response = "Invalid WhiteSpace Error";
            return response;
        }

        if (command.equalsIgnoreCase("help")) {
            return getCommandList();
        }

        String[] commandParts;
        Method method;
        Parameter[] params;
        Object[] args;

        commandParts = command.toLowerCase().split(" ");
        if (commandParts.length == 0){
            return null;
        }
        method = methods.get(commandParts[0]);
        if (method == null) {
            response = "No such command. Try 'Help'.";
            return response;
        }

        params = method.getParameters();
        args = new Object[params.length];

        try {
            for (int i = 0; i < params.length; i++) {
                args[i] = stringToObject(commandParts[i+1], params[i]);
            }
        } catch (NumberFormatException e) {
            response = "Invalid Arguments";
            return response;
        }

        try {
            Object returnArrayObject = method.invoke(this, args);
            response = returnArrayObject == null ? null : ( (String[]) returnArrayObject ) [0];
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            response = "Exception: " + e.getClass().getName();
        }

        return response;
    }

    public String getCommandList() {
        return commandList;
    }

    public String[] getCommandNames() {
        return commandNames;
    }

    // -------------------------------------------------------------------

    private Object stringToObject(String argStr, Parameter p) {
        switch (p.getType().getName()) {
            case "int":
                return Integer.parseInt(argStr);
            case "String":
                return argStr;
            case "double":
                return Double.parseDouble(argStr);
            case "float":
                return Float.parseFloat(argStr);
            case "boolean":
                return Boolean.parseBoolean(argStr);
            default:
                System.out.println("wtf");
                return null;
        }
    }

    private boolean hasInvalidWhitespace(String str) {
        return str.contains("\n") || str.contains("\t") || str.contains("\b") || str.contains("\r");
    }

    private String removeInvalid(String str) {
        return str.replaceAll("\n|\t|\b|\r", "");
    }

    // -------------------------------------------------------------------

    @ConsoleCommand(helpText = "Toggle Fullscreen")
    public void fullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(app.frameWidth, app.frameHeight);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }

    @ConsoleCommand(helpText = "Set 3D Render Resolution")
    public void render_size(int width, int height) {
        //if (w < 1 || h < 1) return;
        app.setGameRenderFrameSize(width, height);
    }

    @ConsoleCommand(helpText = "Toggle Vsync")
    public String[] vsync() {
        String[] response = new String[1];
        response[0] = "Vsync " + (app.toggleVsync() ? "Enabled" : "Disabled");
        return response;
    }

    @ConsoleCommand(helpText = "Return to Desktop")
    public void quit() {
        Gdx.app.exit();
    }

    @ConsoleCommand(helpText = "Toggle Editor")
    public void edit() {
        app.toggleEditor();
    }

    @ConsoleCommand(helpText = "Restart Editor if Malfunctioning")
    public void editor_destroy() {
        app.destroyEditor();
    }

}