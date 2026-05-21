#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float roundedBoxSdf(vec2 point, vec2 halfSize, float radius) {
    vec2 q = abs(point) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    vec2 uv = texCoord0;
    float aspect = max(vertexColor.g * 8.0, 1.0);
    vec2 point = vec2((uv.x - 0.5) * aspect, uv.y - 0.5);
    float radius = clamp(vertexColor.r * 0.5, 0.0, 0.5);
    float thickness = max(vertexColor.b, 0.002);
    float distance = roundedBoxSdf(point, vec2(aspect * 0.5, 0.5), radius);
    float edge = fwidth(distance) * 1.4;
    float outer = 1.0 - smoothstep(-edge, edge, distance);
    float inner = 1.0 - smoothstep(-thickness - edge, -thickness + edge, distance);
    float ring = clamp(outer - inner, 0.0, 1.0);

    if (ring <= 0.0) {
        discard;
    }

    fragColor = vec4(vec3(1.0), vertexColor.a * ColorModulator.a * ring);
}
