package me.wieku.circuits.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Bresenham2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import me.wieku.circuits.api.math.Rectangle
import me.wieku.circuits.api.math.Vector2i
import me.wieku.circuits.render.scene.fit
import me.wieku.circuits.render.screens.Editor
import me.wieku.circuits.world.ClassicWorld
import me.wieku.circuits.world.ElementRegistry
import me.wieku.circuits.world.WorldClipboard

//TODO: Clean this mess
class MapManipulator(val world:ClassicWorld, val camera: OrthographicCamera, val editor: Editor):InputProcessor {

	var toPlace = "wire"
	private var last = Vector2i(-1, -1)
	var position = Vector2i()

	val beginPos = Vector2i()
	val endPos = Vector2i()
	private var beginTMP = Vector2i()
	private var endTMP = Vector2i()


	var rectangle:Rectangle? = null
	var clipboard:WorldClipboard? = null

	var pasteMode = false
	var lineMode = false
	private var afterOperation = false

	override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
		if(editor.stage.mouseMoved(screenX, screenY)) return true
		var vec = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
		position.set(vec.x.toInt(), vec.y.toInt())

		return false
	}

	override fun keyTyped(character: Char): Boolean {
		if(editor.stage.keyTyped(character)) return true
		return false
	}

	override fun scrolled(amount: Int): Boolean {
		if(editor.stage.scrolled(amount)) return true
		var before = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
		camera.zoom = Math.max(0.01f, Math.min(4f, camera.zoom+amount*camera.zoom*0.1f))
		camera.update()
		var after = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
		camera.position.sub(after.sub(before))
		camera.fit(world, editor.stage)
		return false
	}

	override fun keyUp(keycode: Int): Boolean {
		if(editor.stage.keyUp(keycode)) return true
		if(keycode == Input.Keys.A || keycode == Input.Keys.D) {
			lineMode = false
		}
		return false
	}

	override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
		if(editor.stage.touchDragged(screenX, screenY, pointer)) return true

		if(Gdx.input.isKeyPressed(Input.Keys.A) && afterOperation) {
			var upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
			endPos.set(upr.x.toInt(), upr.y.toInt()).clamp(0, 0, world.width-1, world.height-1)
			return true
		} else if(Gdx.input.isKeyPressed(Input.Keys.D) && afterOperation) {
			var upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
			endPos.set(upr.x.toInt(), upr.y.toInt()).clamp(0, 0, world.width-1, world.height-1)
			var angle = endPos.copy().sub(beginPos).angle()

			if(angle >= 315 || angle <=45 || (angle in 135.0..225.0)) {
				endPos.set(endPos.x, beginPos.y)
			} else {
				endPos.set(beginPos.x, endPos.y)
			}
			return true
		}

		if(!pasteMode && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
			if(Gdx.input.isButtonPressed(Input.Buttons.LEFT) && rectangle != null) {
				var upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))

				endPos.set(upr.x.toInt(), upr.y.toInt()).clamp(0, 0, world.width-1, world.height-1)

				beginTMP.set(beginPos)
				endTMP.set(endPos)

				calculate(beginTMP, endTMP)

				rectangle!!.reshape(beginTMP, endTMP)
			}
		} else {
			processTouch(screenX, screenY, pointer, true)
		}
		return false
	}

	private fun calculate(beginPosition: Vector2i, endPosition: Vector2i) {
		if(endPosition.x < beginPosition.x) {
			beginPosition.add(1, 0)
		} else {
			endPosition.add(1, 0)
		}

		if(endPosition.y < beginPosition.y) {
			beginPosition.add(0, 1)
		} else {
			endPosition.add(0, 1)
		}
	}

	override fun keyDown(keycode: Int): Boolean {
		if(editor.stage.keyDown(keycode)) return true
		if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
			if(Gdx.input.isKeyPressed(Input.Keys.C)) {
				makeCopy()
			} else if(Gdx.input.isKeyPressed(Input.Keys.X)){
				makeCut()
			} else if(Gdx.input.isKeyPressed(Input.Keys.V)){
				makePaste()
			} else if(Gdx.input.isKeyPressed(Input.Keys.S)){
				editor.saveFile()
			}
		} else if(Gdx.input.isKeyPressed(Input.Keys.F)) {
			if(rectangle != null){
				world.fill(rectangle!!, toPlace)
			}
		}	else if(rectangle != null && Gdx.input.isKeyPressed(Input.Keys.FORWARD_DEL)) {
			world.clear(rectangle!!)
		}
		return false
	}

	override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		if(editor.stage.touchUp(screenX, screenY, pointer, button)) return true
		if(Gdx.input.isKeyPressed(Input.Keys.A) && afterOperation) {
			var upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
			endPos.set(upr.x.toInt(), upr.y.toInt()).clamp(0, 0, world.width-1, world.height-1)
			lineMode = false
			drawLine(beginPos, endPos, true)
		} else if(Gdx.input.isKeyPressed(Input.Keys.D) && afterOperation) {
			var upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
			endPos.set(upr.x.toInt(), upr.y.toInt()).clamp(0, 0, world.width-1, world.height-1)
			var angle = endPos.copy().sub(beginPos).angle()

			if(angle >= 315 || angle <=45 || (angle in 135.0..225.0)) {
				endPos.set(endPos.x, beginPos.y)
			} else {
				endPos.set(beginPos.x, endPos.y)
			}
			lineMode = false
			drawLine(beginPos, endPos, true)
		} else if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && button == Input.Buttons.LEFT && rectangle != null) {
			var upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
			endPos.set(upr.x.toInt(), upr.y.toInt()).clamp(0, 0, world.width-1, world.height-1)

			beginTMP.set(beginPos)
			endTMP.set(endPos)

			calculate(beginTMP, endTMP)

			rectangle!!.reshape(beginTMP, endTMP)
		}
		afterOperation = false
		return false
	}

	override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		if(editor.stage.touchDown(screenX, screenY, pointer, button)) return true

		if(Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.D)) {
			var upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
			beginPos.set(upr.x.toInt(), upr.y.toInt()).clamp(0, 0, world.width-1, world.height-1)
			endPos.set(upr.x.toInt(), upr.y.toInt()).clamp(0, 0, world.width-1, world.height-1)
			afterOperation = true
			lineMode = true
			return false
		} else if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
			val upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
			if(button == Input.Buttons.LEFT) {
				beginPos.set(upr.x.toInt(), upr.y.toInt()).clamp(0, 0, world.width-1, world.height-1)
				endPos.set(beginPos)
				rectangle = Rectangle(beginPos, endPos)
			} else if(button == Input.Buttons.RIGHT) {
				val element = world.getElement(Vector2i(upr.x.toInt(), upr.y.toInt()))
				if(element != null)
					editor.editElement(element)
				return false
			}
		} else if(pasteMode) {
			if(!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
				if(button == Input.Buttons.LEFT) {
					world.paste(position.copy().sub(clipboard!!.width/2, clipboard!!.height/2), clipboard!!)
					pasteMode = false
					afterOperation = true
				} else {
					pasteMode = false
					afterOperation = true
				}
				return false
			}
		} else if(!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && rectangle != null) {
			rectangle = null
			afterOperation = true
			return false
		}
		if(button == Input.Buttons.MIDDLE) {
			var upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
			var element = world.getElement(Vector2i(upr.x.toInt(), upr.y.toInt()))
			if(element != null) {
				toPlace = ElementRegistry.names[element.javaClass]!!
			}
		}
		processTouch(screenX, screenY, pointer, false)
		return false
	}

	private fun processTouch(screenX: Int, screenY: Int, pointer: Int, dragging: Boolean) {
		if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
			camera.position.sub(Gdx.input.getDeltaX(pointer)*camera.zoom, Gdx.input.getDeltaY(pointer)*camera.zoom, 0f)
			camera.fit(world, editor.stage)
			afterOperation = true
		} else {
			val upr = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
			val res = Vector2i(upr.x.toInt(), upr.y.toInt())
			position.set(res)
			if(afterOperation || pasteMode || (rectangle != null)) return
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

	private val bresenham = Bresenham2()
	private fun drawLine(from: Vector2i, to: Vector2i, place: Boolean) {
		if(from == to) {
			makeAction(from.x, from.y, place)
		} else {
			bresenham.line(from.x, from.y, to.x, to.y).forEach {
				makeAction(it.x, it.y, place)
			}
		}
	}

	private fun makeAction(posx: Int, posy: Int, place: Boolean) {
		if (place) {
			world.placeElement(Vector2i(posx, posy), toPlace)
		} else {
			world.removeElement(Vector2i(posx, posy))
		}
	}

	fun makeCut() {
		if(rectangle != null) {
			clipboard = WorldClipboard.create(rectangle!!, world)
			world.clear(rectangle!!)
		}
	}

	fun makeCopy() {
		if(rectangle != null) {
			clipboard = WorldClipboard.create(rectangle!!, world)
		}
	}

	fun makePaste() {
		if(clipboard != null) {
			pasteMode = true
		}
	}

}