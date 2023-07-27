package app.jerboa.spp.data

import android.util.Log
import app.jerboa.glskeleton.utils.compileGLSLProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.opengl.GLES31 as gl3

abstract class ShaderData(open val vertexShader: String, open val fragmentShader: String)
class Mat4(
    val elements: FloatArray = FloatArray(16){0f}
)

data class Vec2 (
    val x: Float,
    val y: Float
)

data class glUniform<T>(
    var value: T,
    val name: String,
    var location: Int
)

val UNIFORM_INT_REGEX = "uniform int (\\S+);".toRegex()
val UNIFORM_FLOAT_REGEX = "uniform float (\\S+);".toRegex()
val UNIFORM_VEC2_REGEX = "uniform vec2 (\\S+);".toRegex()
val UNIFORM_MAT4_REGEX = "uniform mat4 (\\S+);".toRegex()
val UNIFORM_SAMPLER2D_REGEX = "uniform (\\S+) sampler2D (\\S+);".toRegex()
class Shaders(
    open var vertexShader: String,
    open var fragmentShader: String,
    open var uniforms: MutableMap<String, glUniform<*>> = mutableMapOf(),
    open var glId: Int = 0,
    open var name: String = "unamed"
){

    private var isCompiled: Boolean = false

    fun isCompiled(): Boolean{return isCompiled}

    fun create(){
        if (!isProgram()) {
            glId = gl3.glCreateProgram()
            isCompiled = false
        }
    }

    fun release(){
        if (isProgram()) {
            gl3.glDeleteProgram(glId)
            isCompiled = false
        }
    }

    fun isProgram(): Boolean {
        return gl3.glIsProgram(glId)
    }

    fun compile(){

        if (!isProgram()){
            create()
        }

        parseUniforms()

        compileGLSLProgram(glId, vertexShader, fragmentShader)

        isCompiled = true

        for (uniform in uniforms){
            uniform.value.location = gl3.glGetUniformLocation(glId, uniform.value.name)

            setUniform(uniform.value.name, uniform.value.value!!)
        }

    }

    fun use(){
        if (isProgram()) {
            gl3.glUseProgram(glId)
        }
    }

    fun setUniform(name: String, newValue: Any){

        if (name !in uniforms.keys){
            Log.e("setUniform","attempt to set non-existent uniform $name")
            return
        }

        val oldValueClass = (uniforms[name]!!.value)!!::class
        val newValueClass = newValue::class

        if (oldValueClass != newValueClass){
            Log.e("setUniform","attempt to set uniform $name of shader ${this.name} from type ${oldValueClass.simpleName} to type ${newValueClass.simpleName}")
            return
        }

        when (oldValueClass){
            Int::class -> {
                val v = uniforms[name] as glUniform<Int>
                v.value = newValue as Int
                if (isCompiled()) {
                    use()
                    gl3.glUniform1i(v.location, v.value)
                }
            }
            Float::class -> {
                val v = uniforms[name] as glUniform<Float>
                v.value = newValue as Float
                if (isCompiled()) {
                    use()
                    gl3.glUniform1f(v.location, v.value)
                }
            }
            Vec2::class -> {
                val v = uniforms[name] as glUniform<Vec2>
                v.value = newValue as Vec2
                if (isCompiled()) {
                    use()
                    gl3.glUniform2f(v.location, v.value.x, v.value.y)
                }
            }
            Mat4::class -> {
                val v = uniforms[name] as glUniform<Mat4>
                v.value = newValue as Mat4
                if (isCompiled()) {
                    use()
                    val buffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder())
                        .asFloatBuffer()
                    buffer.put(v.value.elements)
                    buffer.flip()
                    buffer.limit(16)
                    gl3.glUniformMatrix4fv(v.location, 1, false, buffer)
                    buffer.clear()
                }
            }
        }

    }

    fun parseUniforms(): Boolean {

        if ("uniform" !in vertexShader && "uniform" !in fragmentShader){
            return true
        }

        try {

            for (code in listOf(vertexShader,fragmentShader)) {

                val matchesInt = UNIFORM_INT_REGEX.findAll(code)
                val matchesFloat = UNIFORM_FLOAT_REGEX.findAll(code)
                val matchesMat4 = UNIFORM_MAT4_REGEX.findAll(code)
                val matchesVec2 = UNIFORM_VEC2_REGEX.findAll(code)
                val matchesSampler2D = UNIFORM_SAMPLER2D_REGEX.findAll(code)

                for (match in matchesInt) {
                    val name = match.value.split("int").last().split(";").first()
                        .filter { !it.isWhitespace() }
                    uniforms[name] = glUniform<Int>(
                        0,
                        name,
                        0
                    )
                }

                for (match in matchesFloat) {
                    val name = match.value.split("float").last().split(";").first()
                        .filter { !it.isWhitespace() }
                    uniforms[name] = glUniform<Float>(
                        0f,
                        name,
                        0
                    )
                }

                for (match in matchesMat4) {
                    val name = match.value.split("mat4").last().split(";").first()
                        .filter { !it.isWhitespace() }
                    uniforms[name] = glUniform<Mat4>(
                        Mat4(),
                        name,
                        0
                    )
                }

                for (match in matchesVec2) {
                    val name = match.value.split("vec2").last().split(";").first()
                        .filter { !it.isWhitespace() }
                    uniforms[name] = glUniform<Vec2>(
                        Vec2(0f, 0f),
                        name,
                        0
                    )
                }

                for (match in matchesSampler2D) {
                    val name = match.value.split("sampler2D").last().split(";").first()
                        .filter { !it.isWhitespace() }
                    uniforms[name] = glUniform<Int>(
                        0,
                        name,
                        0
                    )
                }
            }

            return true
        }
        catch (e: Error){
            e.message?.let { Log.e("Parsing shader ${this.name}", it) }
            return false
        }
    }
}