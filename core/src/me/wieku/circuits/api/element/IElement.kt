package me.wieku.circuits.api.element

import me.wieku.circuits.api.math.Axis
import me.wieku.circuits.api.state.State
import me.wieku.circuits.api.world.IWorld
import me.wieku.circuits.api.math.Vector2i

interface IElement {

	fun setState(state: State, axis: Axis)
	fun getState(axis: Axis): State

	fun onPlace(position: Vector2i, world: IWorld)
	fun onNeighbourChange(position: Vector2i, world: IWorld)

	fun getIdleColor(): Int
	fun getActiveColor(): Int
}