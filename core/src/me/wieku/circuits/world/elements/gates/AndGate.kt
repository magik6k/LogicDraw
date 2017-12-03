package me.wieku.circuits.world.elements.gates

import me.wieku.circuits.api.element.BasicGate
import me.wieku.circuits.api.math.Axis
import me.wieku.circuits.api.math.Vector2i
import me.wieku.circuits.api.state.State
import me.wieku.circuits.api.world.IWorld

class AndGate(pos: Vector2i): BasicGate(pos) {

	private lateinit var state: State

	override fun update(tick: Long) {
		var calc = inputs.size > 1
		for(i in 0 until inputs.size)
			calc = calc && inputs[i].isActive()

		if(state.isActive() != calc) {
			state.setActive(calc)
			setOut(calc)
		}
	}

	override fun onPlace(world: IWorld) {
		super.onPlace(world)
		state = world.getStateManager()()
	}

	override fun getIdleColor(): Int = 0x00BFA5

	override fun getActiveColor(): Int = 0x1DE9B6

	override fun getColor(): Int = if (state.isActiveD()) getActiveColor() else getIdleColor()

	override fun setState(state: State, axis: Axis) {}

	override fun getState(axis: Axis): State = state
}