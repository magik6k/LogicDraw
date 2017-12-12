package me.wieku.circuits.render.scene.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import me.wieku.circuits.render.scene.Label

class TextTooltip(background: Color, textColor: Color, textSize: Int): InputListener() {
	var tooltipTable: Table = me.wieku.circuits.render.scene.Table(background)
	private set
	var tooltipLabel: Label = Label("", textColor, textSize)

	init {
		tooltipTable.add(tooltipLabel).pad(5f).fill()
		tooltipTable.touchable = null
		tooltipLabel.touchable = null
		tooltipTable.isVisible = false
	}

	fun getListener(text: String) = object: InputListener() {
		override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
			super.enter(event, x, y, pointer, fromActor)
			tooltipTable.isVisible = true
			tooltipLabel.setText(text)
			tooltipTable.pack()
			tooltipTable.setPosition(MathUtils.clamp(Gdx.input.x.toFloat()+5f, 0f, tooltipTable.stage.width-tooltipTable.width), MathUtils.clamp(tooltipTable.stage.height- Gdx.input.y.toFloat()+5f, 0f, tooltipTable.stage.height-tooltipTable.height))
		}

		override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
			if(tooltipTable.isVisible)
				tooltipTable.setPosition(MathUtils.clamp(Gdx.input.x.toFloat()+5f, 0f, tooltipTable.stage.width-tooltipTable.width), MathUtils.clamp(tooltipTable.stage.height- Gdx.input.y.toFloat()+5f, 0f, tooltipTable.stage.height-tooltipTable.height))
			return super.mouseMoved(event, x, y)
		}

		override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
			super.exit(event, x, y, pointer, toActor)
			tooltipTable.isVisible = false
		}
	}

}