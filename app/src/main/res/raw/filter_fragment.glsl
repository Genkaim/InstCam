#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTexCoord;
uniform samplerExternalOES uTexture;

uniform float uGrayscale;   // 0~1
uniform float uVignette;    // 0~1
uniform float uExposure;    // -1~1
uniform float uWarmth;     // -1~1
uniform float uSaturation;  // -1~1
uniform float uTime;

float rand(vec2 co) { return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453); }

void main() {
    vec2 tc = vTexCoord;
    vec3 color = texture2D(uTexture, tc).rgb;

    // 曝光
    color *= pow(2.0, uExposure);

    // 色温
    color.r += uWarmth * 0.15;
    color.b -= uWarmth * 0.15;

    // 饱和度
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(luma), color, 1.0 + uSaturation);

    // 灰度
    color = mix(color, vec3(luma), uGrayscale);

    // 暗角：根据到中心的距离压暗边缘
    if (uVignette > 0.001) {
        vec2 center = vec2(0.5, 0.5);
        float dist = distance(tc, center);
        float dark = 1.0 - uVignette * 0.6 * smoothstep(0.35, 0.7, dist);
        color *= dark;
    }

    gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
