package me.wieku.circuits.world

import com.google.common.eventbus.EventBus
import me.wieku.circuits.api.element.IElement
import me.wieku.circuits.api.element.ITickable
import me.wieku.circuits.api.math.Direction
import me.wieku.circuits.api.math.Rectangle
import me.wieku.circuits.api.math.Vector2i
import me.wieku.circuits.api.state.StateManager
import me.wieku.circuits.api.world.IWorld
import me.wieku.circuits.api.world.clock.AsyncClock
import java.util.*

//TODO: switch to tasks instead of locking objects
class ClassicWorld(val width: Int, val height: Int, val name: String):IWorld {
	private val manager: ClassicStateManager = ClassicStateManager(width * height)
	private val map: Array<Array<IElement?>> = Array(width) { Array<IElement?>(height) {null} }
	private val tickables: HashMap<Vector2i, ITickable> = HashMap()
	private val tasks = ArrayDeque<Runnable>()
	val eventBus = EventBus("buttons")

	var clock: AsyncClock? = null

	var entities = 0
	get() = tickables.size
	private set

	override fun update(tick: Long) {
		updateTasks()
		tickables.entries.forEach { it.value.update(tick) }
		manager.swap()
	}

	fun updateTasks() {
		while(tasks.isNotEmpty()) tasks.poll().run()
	}

	override fun placeElement(position: Vector2i, name: String) {
		if(ElementRegistry.classes.containsKey(name)) {
			placeElement(position, ElementRegistry.classes[name]!!)
		} else {
			println("[ERROR] Element doesn't exist!")
		}
	}

	private fun placeElementNT(position: Vector2i, name: String) {
		if(ElementRegistry.classes.containsKey(name)) {
			placeElementNT(position, ElementRegistry.classes[name]!!)
		} else {
			println("[ERROR] Element doesn't exist!")
		}
	}

	private fun placeElement(position: Vector2i, clazz: Class<out IElement>) {
		tasks.add(Runnable {
			placeElementNT(position, clazz)
		})
	}

	private fun placeElementNT(position: Vector2i, clazz: Class<out IElement>) {
		if(!position.isInBounds(0, 0, width-1, height-1)) return
		if(map[position.x][position.y] != null) {
			if(map[position.x][position.y]!!.javaClass != clazz) {
				removeElementNT(position)
			} else return
		}
		var el:IElement = clazz.getConstructor(Vector2i::class.java).newInstance(position)
		map[position.x][position.y] = el
		el.onPlace(this)
		if(el is ITickable) {
			tickables.put(position, el)
		}
	}

	fun forcePlace(x: Int, y: Int, name: String): IElement {
		if(ElementRegistry.classes.containsKey(name)) {
			var position = Vector2i(x, y)
			var el:IElement = ElementRegistry.classes[name]!!.getConstructor(Vector2i::class.java).newInstance(position)
			map[position.x][position.y] = el
			if(el is ITickable) {
				tickables.put(position, el)
			}
			return el
		} else {
			throw IllegalStateException("Element $name doesn't exist!")
		}
	}

	private var tempVec = Vector2i()
	fun clear(rectangle: Rectangle) {
		tasks.add(Runnable {
			clearNT(rectangle)
		})
	}

	private fun clearNT(rectangle: Rectangle) {
		if(rectangle.width > 2 && rectangle.height > 2) {
			lock = true
			for (x in rectangle.x+1 until rectangle.x + rectangle.width-1) {
				for (y in rectangle.y+1 until rectangle.y + rectangle.height-1) {
					removeElementNT(tempVec.set(x, y))
				}
			}
			lock = false

			for (x in rectangle.x until rectangle.x + rectangle.width) {
				removeElementNT(tempVec.set(x, rectangle.y))
				removeElementNT(tempVec.set(x, rectangle.y + rectangle.height-1))
			}

			for (y in rectangle.y until rectangle.y + rectangle.height) {
				removeElementNT(tempVec.set(rectangle.x, y))
				removeElementNT(tempVec.set(rectangle.x + rectangle.width - 1, y))
			}

		} else {
			for (x in rectangle.x until rectangle.x + rectangle.width) {
				for (y in rectangle.y until rectangle.y + rectangle.height) {
					removeElementNT(tempVec.set(x, y))
				}
			}
		}
	}

	fun fill(rectangle: Rectangle, toPlace: String) {
		tasks.add(Runnable {
			clearNT(rectangle)
			for (x in rectangle.x until rectangle.x + rectangle.width) {
				for (y in rectangle.y until rectangle.y + rectangle.height) {
					placeElementNT(Vector2i(x, y), toPlace)
				}
			}
		})
	}

	fun paste(position: Vector2i, clipboard: WorldClipboard) {
		tasks.add(Runnable {
			pasteNT(position, clipboard)
		})
	}

	fun pasteNT(position: Vector2i, clipboard: WorldClipboard) {
		clearNT(Rectangle(position, Vector2i(position.x+clipboard.width, position.y + clipboard.height)))
		for(x in 0 until clipboard.width) {
			for (y in 0 until clipboard.height) {
				if(clipboard[x, y] != null)
					placeElementNT(Vector2i(x, y).add(position), clipboard[x, y]!!.javaClass)
				else
					removeElementNT(Vector2i(x, y).add(position))
			}
		}
	}

	override fun removeElement(position: Vector2i) {
		tasks.add(Runnable {
			removeElementNT(position)
		})
	}

	private fun removeElementNT(position: Vector2i) {
		if(!position.isInBounds(0, 0, width-1, height-1)) return
		var element: IElement? = map[position.x][position.y] ?: return

		tickables.remove(position)
		map[position.x][position.y] = null
		element!!.onRemove(this)
	}

	override fun getElement(position: Vector2i) = if(position.isInBounds(0, 0, width - 1, height - 1)) map[position.x][position.y] else null

	private fun getNeighboursOf(position: Vector2i): Array<IElement> {
		var list = ArrayList<IElement>()
		Direction.VALID_DIRECTIONS.forEach {
			var tmpE = getElement(position + it.asVector)
			if (tmpE != null) list.add(tmpE)
		}
		return list.toTypedArray()
	}

	override fun getNeighboursOf(element: IElement): Array<IElement> = getNeighboursOf(element.getPosition())

	override fun getNeighboursOf(element: IElement, consumer: (IElement) -> Unit) = getNeighboursOf(element).forEach(consumer)

	private val stack = ArrayDeque<Vector2i>()
	private var first = true
	private var tempVector = Vector2i()
	private var lock = false
	override fun updateNeighboursOf(pos: Vector2i) {
		if(lock) return
		stack.push(pos)
		if(first) {
			first = false
			while(stack.isNotEmpty()) {
				val position = stack.pop()
				for(i in 0 until Direction.VALID_DIRECTIONS.size) {
					tempVector.set(position).add(Direction.VALID_DIRECTIONS[i].asVector)
					val tmpE = getElement(tempVector)
					if (tmpE != null) tmpE.onNeighbourChange(position, this)
				}
			}
			first = true
		}
	}

	override fun getStateManager(): StateManager = manager

	operator fun get(x:Int, y:Int) = map[x][y]
}