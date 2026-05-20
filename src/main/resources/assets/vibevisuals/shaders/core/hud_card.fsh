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
    vec2 point = vec2((uv.x - 0.5) * 2.5, (uv.y - 0.5) * 1.0);
    float distance = roundedBoxSdf(point, vec2(1.25, 0.5), 0.105);
    float edge = fwidth(distance);
    float mask = 1.0 - smoothstep(-edge, edge, distance);

    if (mask <= 0.0) {
        discard;
    }

    float topLight = 1.0 - uv.y;
    float vignette = smoothstep(1.12, 0.25, length(point * vec2(0.72, 1.55)));
    vec3 base = mix(vec3(0.035, 0.039, 0.055), vec3(0.075, 0.079, 0.098), topLight);
    base += vec3(0.018, 0.016, 0.035) * vignette;

    float border = 1.0 - smoothstep(0.0, edge * 1.7, abs(distance));
    vec3 color = base + border * vec3(0.040, 0.043, 0.060);

    float alpha = vertexColor.a * ColorModulator.a * mask;
    fragColor = vec4(color, alpha);
}
