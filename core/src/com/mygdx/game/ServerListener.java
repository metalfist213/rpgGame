/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mygdx.game;

import Server.ExtraCommand;
import Server.WorldSettings;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.mygdx.game.Packets.BeginMovement;
import com.mygdx.game.Packets.EndMovement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kristian
 */
public class ServerListener extends Listener {

    private ArrayList<Connection> connections;
    private HashMap<Connection, Player> playerToConnection;
    private GameWorld world;
    private WorldSettings worldSettings;
    private String fullPath;
    private Server server;
    private int fieldOfView = 2; //default 1
    private boolean isRecurring;

    public ServerListener(Server server, GameWorld gameWorld, String fullPath) {
        this.fullPath = fullPath;
        this.server = server;
        connections = new ArrayList();
        playerToConnection = new HashMap();
        world = gameWorld;
        worldSettings = new WorldSettings("Test", world.getSizeX(), world.getSizeY());
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof Packets.Message) {
            Packets.Message obj = (Packets.Message) object;
            System.out.println("Message received!: " + obj.message);
        }
        if (object instanceof Packets.RequestAccess) {
            Packets.RequestAccess obj = (Packets.RequestAccess) object;
            Player player = null;
            player = Player.get(fullPath + "players/", obj.name, obj.password);

            if (player != null) {
                connection.sendTCP(player);
                connection.sendTCP(worldSettings);
                playerToConnection.put(connection, player);
                sendWorldData(connection, player);
            }
            sendAllPlayerInfo();
        }
        if (object instanceof Packets.BeginMovement) {
            EntitySimpleType player = (EntitySimpleType) ((BeginMovement) object).entity;
            server.sendToAllExceptTCP(connection.getID(), player);
        }
        if (object instanceof Packets.EndMovement) {
            EntitySimpleType player = (EntitySimpleType) ((EndMovement) object).entity;
            playerToConnection.get(connection).setX(player.x);
            playerToConnection.get(connection).setY(player.y);
            server.sendToAllExceptTCP(connection.getID(), player);
        }
        if (object instanceof Packets.requestChunks) {
            Packets.requestChunks request = (Packets.requestChunks) object;
            Player player = playerToConnection.get(connection);
            player.chunkX = request.entity.chunkX;
            player.chunkY = request.entity.chunkY;
            this.isRecurring = false; //Makes sure it only recures once.
            sendWorldData(connection, player);
        }
        if (object instanceof Entity) {
            if(object instanceof Player) {
                playerToConnection.put(connection, (Player) object);
            }
            server.sendToAllExceptTCP(connection.getID(), object);
        }
        if (object instanceof WorldObject) { //If the object has been modified
            WorldObject worldObject = (WorldObject) object;
            world.updateWorldObject(worldObject);
            
            //--------------Add update to server!!!
            
            
            server.sendToAllExceptTCP(connection.getID(), object);  
        }
    }

    public void sendWorldData(Connection connection, Player player) {
        //Disable editing on all of the other.
        for (Chunk chunk : world.getChunks()) {
            if (chunk.getClientControlling() != null) {
                if (chunk.getClientControlling().equals(player.getUId())) {
                    chunk.setClientControlling(null);
                }
            }
        }

        //Set new Access
        for (int iy = -fieldOfView; iy < fieldOfView + 1; iy++) {
            for (int ix = -fieldOfView; ix < fieldOfView + 1; ix++) {
                Chunk chunk = world.getChunk(ix + player.chunkX, iy + player.chunkY);
                if (chunk != null) {
                    if (chunk.getClientControlling() == null) {
                        chunk.setClientControlling(player.uId);
                    }
                    connection.sendTCP(chunk);
                }
            }
        }

        System.out.println("Player: "+player.getUsername());
        //Set access for all the other clients
        if (!isRecurring) {
            isRecurring = true;
            for (Connection conn : connections) {
                if (conn != connection) {
                    sendWorldData(conn, playerToConnection.get(conn));
                }
            }
        }
        System.out.println("has send data");
    }

    public void removeClient(Connection connection) {
        Player player = playerToConnection.get(connection);
        isRecurring = false;
        //Disable editing on all of the other.
        for (Chunk chunk : world.getChunks()) {
            if (chunk.getClientControlling() != null) {
                if (chunk.getClientControlling().equals(player.getUId())) {
                    chunk.setClientControlling(null);
                }
            }
        }

        player.extraCommand = ExtraCommand.LOGOUT;
        server.sendToAllTCP(player);
        playerToConnection.remove(connection);
        connections.remove(connection);
        //Set access for all the other clients
        if (connections.size() != 0) {
            if (connections.get(0) != null) { //Updates the permissions
                sendWorldData(connections.get(0), playerToConnection.get(connections.get(0)));
            }
        }
    }

    public void sendAllPlayerInfo() {
        Set<Connection> players = playerToConnection.keySet();
        for (Connection connection : players) {
            server.sendToAllTCP(playerToConnection.get(connection));
        }
    }

    @Override
    public void connected(Connection connection) {
        connections.add(connection);
    }

    @Override
    public void disconnected(Connection connection) {
        removeClient(connection);
    }
}
