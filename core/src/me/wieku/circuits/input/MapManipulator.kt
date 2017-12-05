package me.wieku.circuits.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Bresenham2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import me.wieku.circuits.api.math.Vector2i
import me.wieku.circuits.render.utils.fit
import me.wieku.circuits.world.ClassicWorld

class MapManipulator(val world:ClassicWorld, val camera: OrthographicCamera, val stage: Stage):InputProcessor {

	var toPlace = "Wire"
	private var last = Vector2i(-1, -1)
	var position = Vector2i()

	override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		if(stage.touchUp(screenX, screenY, pointer, button)) return true
		return false
	}

	override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
		if(stage.mouseMoved(screenX, screenY)) return true
		var vec = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
		position.set(vec.x.toInt(), vec.y.toInt())
		return false
	}

	override fun keyTyped(character: Char): Boolean {
		if(stage.keyTyped(character)) return true
		return false
	}

	override fun scrolled(amount: Int): Boolean {
		if(stage.scrolled(amount)) return true
		if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
			var before = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
			camera.zoom = Math.max(0.01f, Math.min(4f, camera.zoom+amount*camera.zoom*0.1f))
			camera.update()
			var after = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
			camera.position.sub(after.sub(before))
			camera.fit(world, stage)
		}
		return false
	}

	override fun keyUp(keycode: Int): Boolean {
		if(stage.keyUp(keycode)) return true
		return false
	}

	override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
		if(stage.touchDragged(screenX, screenY, pointer)) return true
		processTouch(screenX, screenY, pointer, true)
		return false
	}

	override fun keyDown(keycode: Int): Boolean {
		if(stage.keyDown(keycode)) return true
		return false
	}

	override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		if(stage.touchDown(screenX, screenY, pointer, button)) return true
		processTouch(screenX, screenY, pointer, false)
		return false
	}

	private fun processTouch(screenX: Int, screenY: Int, pointer: Int, dragging: Boolean) {
		if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
			camera.position.sub(Gdx.input.getDeltaX(pointer)*camera.zoom, Gdx.input.getDeltaY(pointer)*camera.zoom, 0f)
			camera.fit(world, stage)
		} else {
			var upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
			var res = Vector2i(upr.x.toInt(), upr.y.toInt())
			position.set(res)
			if(dragging) {
				if(res == last)
					return
			} else last.set(res)
			if(Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
				drawLine(last, res, true)
			} else if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
				drawLine(last, res, false)
			}
			last.set(res)
		}
	}

	var bresenham = Bresenham2()
	private fun drawLine(from: Vector2i, to: Vector2i, place: Boolean) {
		if(from == to) makeAction(from.x, from.y, place)

		bresenham.line(from.x, from.y, to.x, to.y).forEach {
			makeAction(it.x, it.y, place)
		}

	}

	private fun makeAction(posx: Int, posy: Int, place: Boolean) {
		if (place) {
			world.placeElement(Vector2i(posx, posy), toPlace)
		} else {
			world.removeElement(Vector2i(posx, posy))
		}
	}

}