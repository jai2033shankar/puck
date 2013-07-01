package puck.util

import com.nativelibs4java.opencl._
import org.bridj.Pointer

case class CLBufferMappedPointerPair[V](val buffer: CLBuffer[V])(implicit queue: CLQueue) {
  private var _mappedPointer:Pointer[V] = null
  
  def mappedPointer = synchronized {
    if(_mappedPointer eq null) {
      _mappedPointer = buffer.map(queue, CLMem.MapFlags.ReadWrite)
    } 
    _mappedPointer
  }
  
  def unmap(evs: CLEvent*) = synchronized {
    if(_mappedPointer ne null) { 
      val ev = buffer.unmap(queue, _mappedPointer, evs:_*)
      val mm = _mappedPointer
      ev.invokeUponCompletion(new Runnable() {
        def run() {  mm.release() }
        })
      _mappedPointer = null
      ev
    } else {
      queue.enqueueWaitForEvents(evs:_*)
      null
    }
  }

  def waitUnmap() {
    Option(unmap()).foreach(_.waitFor)
  }

  def release() {
    waitUnmap()
    buffer.release()
  }

  def safeBuffer = { waitUnmap(); buffer }
}

object CLBufferMappedPointerPair {
  implicit def fromBuffer[V](buffer: CLBuffer[V])(implicit queue: CLQueue):CLBufferMappedPointerPair[V] = CLBufferMappedPointerPair[V](buffer)
  implicit def toBuffer[V](buffer: CLBufferMappedPointerPair[V]):CLBuffer[V] = buffer.safeBuffer
}