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
    float radius = clamp(vertexColor.r * 0.5, 0.004, 0.22);
    float distance = roundedBoxSdf(point, vec2(aspect * 0.5, 0.5), radius);
    float edge = fwidth(distance);
    float mask = 1.0 - smoothstep(-edge, edge, distance);

    if (mask <= 0.0) {
        discard;
    }

    float topLight = 1.0 - uv.y;
    float vignette = smoothstep(1.10, 0.18, length(point * vec2(0.70, 1.45)));
    vec3 base = mix(vec3(0.035, 0.039, 0.052), vec3(0.020, 0.023, 0.032), uv.y);
    base += vec3(0.020, 0.022, 0.034) * topLight * 0.30;
    base += vec3(0.010, 0.012, 0.020) * vignette;

    float border = 1.0 - smoothstep(0.0, edge * 1.4, abs(distance));
    vec3 color = base + border * vec3(0.010, 0.012, 0.018);

    float alpha = vertexColor.a * ColorModulator.a * mask;
    fragColor = vec4(color, alpha);
}
