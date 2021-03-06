/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mygdx.game;

import Persistence.Action;
import Persistence.GameObject;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.esotericsoftware.kryonet.Client;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author kristian
 */
public class WorldObject implements Drawable {

    protected int x, y, z;
    protected int id;
    protected boolean toBeRemoved;
    protected String name;
    protected String description;
    protected Rectangle rectangle;
    protected String uId;
    private String uIdOfTown;
    protected static Client client;
    private float updateTimer;
    private GameWorld world;
    private Body body;

    public WorldObject(GameWorld world, int id, int x, int y) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.uId = UUID.randomUUID().toString();
        this.world = world;
        //initialize();
    }

    public void initialize() {
        if (this.body != null) {
            this.body.setActive(false);
        }
        if (id != -1) {

            if (Game.objectManager.getGameObject(id) != null && Game.objectManager.getGameObject(id).getBounds() != null) {
                this.rectangle = new Rectangle(Game.objectManager.getGameObject(id).getBounds().x + x, //Set's the bounds to be like the one in the GameObject
                        Game.objectManager.getGameObject(id).getBounds().y + y,
                        Game.objectManager.getGameObject(id).getBounds().width,
                        Game.objectManager.getGameObject(id).getBounds().height);
                if (this.body == null) {
                    this.createBody();
                } else {
                    this.body.setActive(true);
                }
            } else {
                this.rectangle = null;
            }
            this.updateTimer = Game.objectManager.getGameObject(id).getRespawnTime();
        } else {
            this.toBeRemoved = true;

        }
    }

    public void update(double deltaTime) {
        if (id != -1) {
            Game.objectManager.getGameObject(id).update(this, deltaTime);
        }
    }

    public void setId(int id) {
        this.id = id;

        //Updates the object.
        this.initialize();
        sendUpdate();
    }

    public void sendUpdate() {
        GameWorld worldHold = this.world;
        this.world = null;
        Body bodyhold = body;
        this.body = null;
        WorldObject.client.sendTCP(this);
        this.body = bodyhold;
        this.world = worldHold;
        initialize();
    }

    public WorldObject() {
        this.uId = UUID.randomUUID().toString();
    }

    public static void setClient(Client client) {
        WorldObject.client = client;
    }

    public static WorldObject create(GameWorld world, int id, int x, int y) {
        WorldObject temp = new WorldObject(world, id, x, y);
        return temp;
    }

    @Override
    public void draw() {
        Player p = this.world.getPlayer();
        float distance = Vector2.dst(p.getX(), p.getY(), this.x, this.y);
        if (distance < Game.settings.getViewDistance() && distance > Game.settings.getViewDistance() * 0.9) {
            float min = distance - Game.settings.getViewDistance() * 0.9f;
            float max = Game.settings.getViewDistance() - Game.settings.getViewDistance() * 0.9f;
            float closeness = min / max;
            Game.batch.setColor(1, 1, 1, 1 - closeness);
        }
        if (distance < Game.settings.getViewDistance()) {
            if (id != -1) {

                if (!Game.objectManager.getGameObject(id).isGhostObject()) {
                    boolean hasPermissionToSeeAll = p.isXray() ? this.getPermissionLevel(p.getUId()) > 0 : false;
                    Game.objectManager.getGameObject(id).draw(x, y, hasPermissionToSeeAll);
                } else if (this.getPermissionLevel(p.getUId()) >= 2) {
                    Game.batch.setColor(0.2f, 0.4f, 0.5f, 0.6f);
                    Game.objectManager.getGameObject(id).draw(x, y, false);
                }
            }
        }
        Game.batch.setColor(Color.WHITE);
    }

    @Override
    public void drawShadow() {
        Player p = this.world.getPlayer();
        float distance = Vector2.dst(p.getX(), p.getY(), this.x, this.y);
        if (distance < Game.settings.getViewDistance()) {
            Game.objectManager.getGameObject(id).drawShadow(x, y, false);
        }
    }

    public void drawLights() {
        if (id != -1) {
            Game.objectManager.getGameObject(id).drawLights(x, y);
        }
    }

    @Override
    public float getY() {
        return y;
    }

    public ArrayList<Action> getActions(String uId) {
        int permissionLevel = this.getPermissionLevel(uId);
        ArrayList<Action> returnActions = new ArrayList();
        for (Action action : Game.objectManager.getGameObject(id).getActions()) {
            if (action.getPermissionLevel() <= permissionLevel) {
                returnActions.add(action);
            }
        }
        return returnActions;
    }

    @Override
    public ArrayList<Action> getActions() {
        return Game.objectManager.getGameObject(id).getActions();
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public String toString() {
        return Game.objectManager.getGameObject(id).getName();
    }

    public String getUid() {
        return uId;
    }

    void generateUId() {
        this.uId = UUID.randomUUID().toString();
    }

    /**
     * @return the updateTimer
     */
    public float getUpdateTimer() {
        return updateTimer;
    }

    /**
     * @param updateTimer the updateTimer to set
     */
    public void setUpdateTimer(float updateTimer) {
        this.updateTimer = updateTimer;
    }

    @Override
    public boolean isFlaggedForRemoval() {
        return toBeRemoved;
    }

    public int getPermissionLevel(String uId) {
        if (this.uIdOfTown == null) {
            Iterator townIterator = this.world.getTowns().entrySet().iterator();
            while (townIterator.hasNext()) {
                Town town = (Town) ((Map.Entry) townIterator.next()).getValue();

                //create temporary rectangle if it doesn't have one
                Rectangle rectangle;
                if (this.rectangle == null) {
                    rectangle = new Rectangle(this.x, this.y, 32, 32);
                } else {
                    rectangle = this.rectangle;
                }

                if (rectangle.overlaps(town.getBounds())) {
                    this.uIdOfTown = town.getuId();
                }
            }
        } else {
            Town town = world.getTowns().get(this.uIdOfTown);
            for (String string : town.getOwnersOfPoint(x, y)) {
                if (string.equals(uId)) {
                    return 3;
                }
            }
            for (String string : town.getBuildersOfPoint(x, y)) {
                if (string.equals(uId)) {
                    return 2;
                }
            }
            for (String string : town.getGuestsOfPoint(x, y)) {
                if (string.equals(uId)) {
                    return 1;
                }
            }
        }
        return 0;
    }

    public void setWorld(GameWorld world) {
        this.world = world;
    }

    @Override
    public float getZ() {
        if (Game.objectManager.getGameObject(id) != null) {
            return Game.objectManager.getGameObject(id).getzIndex();
        }
        return 0;
    }

    public void createBody() {
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.StaticBody;
        bdef.position.set(new Vector2(x, y));

        Body body = world.getPhysicsWorld().createBody(bdef);
        CircleShape circle = new CircleShape();
        circle.setRadius(16f);

        FixtureDef fixture = new FixtureDef();
        fixture.shape = circle;

        body.createFixture(fixture);

        this.body = body;

        circle.dispose();
    }
}
