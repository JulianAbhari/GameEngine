package com.Julian.game.entities;

import com.Julian.game.Game;
import com.Julian.game.InputHandler;
import com.Julian.game.gfx.Colors;
import com.Julian.game.gfx.Font;
import com.Julian.game.gfx.Screen;
import com.Julian.game.level.Level;
import com.Julian.game.level.tiles.BasicInteractiveTile;
import com.Julian.game.level.tiles.BasicTriggerTile;
import com.Julian.game.net.packets.Packet02Move;

public class Player extends Mob {

	private InputHandler input;
	private int color = Colors.get(-1, 111, 259, 543);
	private int scale = 1;
	protected boolean isSwimming = false;
	protected boolean nearInteractive = true;
	private int tickCount = 0;
	private String username;

	public int xFace = 0;
	public int yFace = 0;

	public Player(Level level, int x, int y, InputHandler input, String username) {
		super(level, "Player", x, y, 1);
		this.input = input;
		this.username = username;
	}

	// This updates the game, it updates the internal variables and the logic of the
	// game
	public void tick() {
		int xDir = 0;
		int yDir = 0;

		if (input != null) {
			if (input.up.isPressed()) {
				yDir -= 1;
				yFace = -1;
				xFace = 0;
			}
			if (input.down.isPressed()) {
				yDir += 1;
				yFace = 1;
				xFace = 0;
			}
			if (input.left.isPressed()) {
				xDir -= 1;
				xFace = -1;
				yFace = 0;
			}
			if (input.right.isPressed()) {
				xDir += 1;
				xFace = 1;
				yFace = 0;
			}
			// Check if the player is trying to interact with an interactive tile
			if (input.D.isPressed() && level.getTile((this.x >> 3) + xFace, (this.y >> 3) + yFace).isInteractive()) {
				((BasicInteractiveTile) level.getTile((this.x >> 3) + xFace, (this.y >> 3) + yFace)).doAction();
			}
		}
		if (xDir != 0 || yDir != 0) {
			move(xDir, yDir);
			isMoving = true;
			
			Packet02Move packet = new Packet02Move(this.getUsername(), this.x, this.y, this.numSteps, this.isMoving, this.movingDir);
			packet.writeData(Game.game.socketClient);
			
		} else {
			isMoving = false;
		}

		// Check if the player is near an interactive tile, if so, then pull up
		// interacting prompt
		if (level.getTile((this.x >> 3) + xFace, (this.y >> 3) + yFace).isInteractive()) {
			nearInteractive = true;
		}
		// Set nearInteractive to false if the player isn't near an interactive tile
		// anymore
		if (!(level.getTile((this.x >> 3) + xFace, (this.y >> 3) + yFace).isInteractive())) {
			nearInteractive = false;
		}

		// Check if the player is on a trigger tile
		if (level.getTile(this.x >> 3, this.y >> 3).isTrigger()) {
			// The tile will be triggered if the player is on it and the tile can be
			// triggered
			((BasicTriggerTile) level.getTile(this.x >> 3, this.y >> 3)).doAction();
		}

		// Check if the player is trying to get in the water
		if (level.getTile((this.x >> 3), (this.y >> 3)).getId() == 3) {
			isSwimming = true;
		}
		// Check if the player is trying to get out of the water
		if (isSwimming && level.getTile((this.x >> 3), (this.y >> 3)).getId() != 3) {
			isSwimming = false;
		}
		tickCount += 1;
	}

	public void render(Screen screen) {
		int xTile = 0;
		int yTile = 28;
		int walkingSpeed = 4;
		int flipTop = (numSteps >> walkingSpeed) & 1;
		int flipBottom = (numSteps >> walkingSpeed) & 1;

		// When the player is facing towards the camera the x place for getting the Tile
		// pixels increases by 2 (because the player is 2 tiles wide)
		if (movingDir == 1) {
			xTile += 2;
			flipTop = (movingDir - 1) % 2;
		} else if (movingDir > 1) {
			xTile += 4 + ((numSteps >> walkingSpeed) & 1) * 2;
			flipTop = (movingDir - 1) % 2;
			flipBottom = (movingDir - 1) % 2;
		}

		int modifier = 8 * scale;
		int xOffset = x - modifier / 2;
		int yOffset = y - modifier / 2 - 4;

		if (isSwimming) {
			int waterColor = 0;
			yOffset += 4;
			if (tickCount % 60 < 15) {
				waterColor = Colors.get(-1, -1, 225, -1);
			} else if (tickCount % 60 >= 15 && tickCount % 60 < 30) {
				yOffset -= 1;
				waterColor = Colors.get(-1, 225, 115, -1);
			} else if (tickCount % 60 >= 30 && tickCount % 60 < 45) {
				waterColor = Colors.get(-1, 115, -1, 225);
			} else {
				yOffset -= 1;
				waterColor = Colors.get(-1, 225, 115, -1);
			}
			screen.render(xOffset, yOffset + 3, 0 + 27 * 32, waterColor, 1);
			screen.render(xOffset + 8, yOffset + 3, 0 + 27 * 32, waterColor, true, false, 1);
		}

		screen.render(xOffset + (modifier * flipTop), yOffset, xTile + yTile * 32, color, flipTop == 1, false, scale);
		screen.render(xOffset + modifier - (modifier * flipTop), yOffset, (xTile + 1) + yTile * 32, color, flipTop == 1,
				false, scale);

		if (!isSwimming) {
			screen.render(xOffset + (modifier * flipBottom), yOffset + modifier, xTile + (yTile + 1) * 32, color,
					flipBottom == 1, false, scale);
			screen.render(xOffset + modifier - (modifier * flipBottom), yOffset + modifier,
					(xTile + 1) + (yTile + 1) * 32, color, flipBottom == 1, false, scale);
		}

		if (nearInteractive) {
			screen.render(xOffset - 8, yOffset, 1 + 27 * 32, Colors.get(-1, 323, 452, 555), 1);
		}

		if (username != null) {
			Font.render(username, screen, xOffset - (((username.length() - 1) / 2) * 8), yOffset - 10,
					Colors.get(-1, -1, -1, 555), 1);
		}
	}

	public boolean hasCollided(int xDir, int yDir) {
		int xMin = 0;
		int xMax = 7;
		int yMin = 0;
		int yMax = 7;
		for (int x = xMin; x < xMax; x += 1) {
			if (isSolidTile(xDir, yDir, x, yMin)) {
				return true;
			}
		}
		for (int x = xMin; x < xMax; x += 1) {
			if (isSolidTile(xDir, yDir, x, yMax)) {
				return true;
			}
		}
		for (int y = yMin; y < yMax; y += 1) {
			if (isSolidTile(xDir, yDir, xMin, y)) {
				return true;
			}
		}
		for (int y = yMin; y < yMax; y += 1) {
			if (isSolidTile(xDir, yDir, xMax, y)) {
				return true;
			}
		}
		return false;
	}

	public String getUsername() {
		return this.username;
	}

}
