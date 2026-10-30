#version 150

// Full-screen quad (action plan 197, section 4.1). The view direction is rebuilt per pixel in the
// fragment shader from the normalised device coordinates passed on here.
in vec3 Position;

out vec2 texCoord;
out vec2 ndc;

void main() {
    ndc = Position.xy;
    texCoord = Position.xy * 0.5 + 0.5;
    gl_Position = vec4(Position.xy, 1.0, 1.0);
}
