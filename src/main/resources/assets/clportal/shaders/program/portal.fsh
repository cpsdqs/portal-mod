#version 120

varying vec2 tex_coord;
uniform float time;
uniform float open_anim;
uniform float link_anim;
uniform vec2 portal_size;
uniform vec3 portal_color;

// perlin noise
// from https://gist.github.com/patriciogonzalezvivo/670c22f3966e662d2f83
#define M_PI 3.14159265358979323846

vec4 permute(vec4 x){return mod(((x*34.0)+1.0)*x, 289.0);}
vec4 taylorInvSqrt(vec4 r){return 1.79284291400159 - 0.85373472095314 * r;}
vec3 fade(vec3 t) {return t*t*t*(t*(t*6.0-15.0)+10.0);}

float perlin(vec3 P) {
  vec3 Pi0 = floor(P); // Integer part for indexing
  vec3 Pi1 = Pi0 + vec3(1.0); // Integer part + 1
  Pi0 = mod(Pi0, 289.0);
  Pi1 = mod(Pi1, 289.0);
  vec3 Pf0 = fract(P); // Fractional part for interpolation
  vec3 Pf1 = Pf0 - vec3(1.0); // Fractional part - 1.0
  vec4 ix = vec4(Pi0.x, Pi1.x, Pi0.x, Pi1.x);
  vec4 iy = vec4(Pi0.yy, Pi1.yy);
  vec4 iz0 = Pi0.zzzz;
  vec4 iz1 = Pi1.zzzz;

  vec4 ixy = permute(permute(ix) + iy);
  vec4 ixy0 = permute(ixy + iz0);
  vec4 ixy1 = permute(ixy + iz1);

  vec4 gx0 = ixy0 / 7.0;
  vec4 gy0 = fract(floor(gx0) / 7.0) - 0.5;
  gx0 = fract(gx0);
  vec4 gz0 = vec4(0.5) - abs(gx0) - abs(gy0);
  vec4 sz0 = step(gz0, vec4(0.0));
  gx0 -= sz0 * (step(0.0, gx0) - 0.5);
  gy0 -= sz0 * (step(0.0, gy0) - 0.5);

  vec4 gx1 = ixy1 / 7.0;
  vec4 gy1 = fract(floor(gx1) / 7.0) - 0.5;
  gx1 = fract(gx1);
  vec4 gz1 = vec4(0.5) - abs(gx1) - abs(gy1);
  vec4 sz1 = step(gz1, vec4(0.0));
  gx1 -= sz1 * (step(0.0, gx1) - 0.5);
  gy1 -= sz1 * (step(0.0, gy1) - 0.5);

  vec3 g000 = vec3(gx0.x,gy0.x,gz0.x);
  vec3 g100 = vec3(gx0.y,gy0.y,gz0.y);
  vec3 g010 = vec3(gx0.z,gy0.z,gz0.z);
  vec3 g110 = vec3(gx0.w,gy0.w,gz0.w);
  vec3 g001 = vec3(gx1.x,gy1.x,gz1.x);
  vec3 g101 = vec3(gx1.y,gy1.y,gz1.y);
  vec3 g011 = vec3(gx1.z,gy1.z,gz1.z);
  vec3 g111 = vec3(gx1.w,gy1.w,gz1.w);

  vec4 norm0 = taylorInvSqrt(vec4(dot(g000, g000), dot(g010, g010), dot(g100, g100), dot(g110, g110)));
  g000 *= norm0.x;
  g010 *= norm0.y;
  g100 *= norm0.z;
  g110 *= norm0.w;
  vec4 norm1 = taylorInvSqrt(vec4(dot(g001, g001), dot(g011, g011), dot(g101, g101), dot(g111, g111)));
  g001 *= norm1.x;
  g011 *= norm1.y;
  g101 *= norm1.z;
  g111 *= norm1.w;

  float n000 = dot(g000, Pf0);
  float n100 = dot(g100, vec3(Pf1.x, Pf0.yz));
  float n010 = dot(g010, vec3(Pf0.x, Pf1.y, Pf0.z));
  float n110 = dot(g110, vec3(Pf1.xy, Pf0.z));
  float n001 = dot(g001, vec3(Pf0.xy, Pf1.z));
  float n101 = dot(g101, vec3(Pf1.x, Pf0.y, Pf1.z));
  float n011 = dot(g011, vec3(Pf0.x, Pf1.yz));
  float n111 = dot(g111, Pf1);

  vec3 fade_xyz = fade(Pf0);
  vec4 n_z = mix(vec4(n000, n100, n010, n110), vec4(n001, n101, n011, n111), fade_xyz.z);
  vec2 n_yz = mix(n_z.xy, n_z.zw, fade_xyz.y);
  float n_xyz = mix(n_yz.x, n_yz.y, fade_xyz.x);
  return 2.2 * n_xyz;
}

// cheap fractal noise
// from https://www.shadertoy.com/view/XtsXRn
float __f_noise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f*f*(3.-2.*f);

    float n = p.x + p.y*157. + 113.*p.z;

    vec4 v1 = fract(753.5453123*sin(n + vec4(0., 1., 157., 158.)));
    vec4 v2 = fract(753.5453123*sin(n + vec4(113., 114., 270., 271.)));
    vec4 v3 = mix(v1, v2, f.z);
    vec2 v4 = mix(v3.xy, v3.zw, f.y);
    return mix(v4.x, v4.y, f.x);
}

float fractal_noise(vec3 p) {
  // random rotation reduces artifacts
  p = mat3(0.28862355854826727, 0.6997227302779844, 0.6535170557707412,
           0.06997493955670424, 0.6653237235314099, -0.7432683571499161,
           -0.9548821651308448, 0.26025457467376617, 0.14306504491456504)*p;
  return dot(vec4(__f_noise(p), __f_noise(p*2.), __f_noise(p*4.), __f_noise(p*8.)),
             vec4(0.5, 0.25, 0.125, 0.06));
}

// voronoi noise
// from https://www.shadertoy.com/view/ldl3Dl
vec3 hash(vec3 x) {
    x = vec3(dot(x,vec3(127.1,311.7, 74.7)),
            dot(x,vec3(269.5,183.3,246.1)),
            dot(x,vec3(113.5,271.9,124.6)));

    return fract(sin(x)*43758.5453123);
}

// returns closest, second closest, and cell id
vec3 voronoi(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);

    float id = 0.0;
    vec2 res = vec2(100.0);
    for(int k=-1; k<=1; k++)
    for(int j=-1; j<=1; j++)
    for(int i=-1; i<=1; i++)
    {
        vec3 b = vec3(float(i), float(j), float(k));
        vec3 r = vec3(b) - f + hash(p + b);
        float d = dot(r, r);

        if(d < res.x) {
            id = dot(p + b, vec3(1.0, 57.0, 113.0));
            res = vec2(d, res.x);
        } else if(d < res.y) {
            res.y = d;
        }
    }

    return vec3(sqrt(res), abs(id));
}

vec4 color_at(vec2 tex_coord, float fractal_value) {
    float voronoi_value = voronoi(vec3(tex_coord, time / 50.)).x;
    float perlin_value = perlin(vec3(tex_coord * 3., time / 50.));

    vec4 color = vec4(portal_color, 1);

    vec2 normalized_pos = tex_coord / portal_size * 2.;
    float edge_dist = length(normalized_pos);

    vec4 base_noise_color = mix(vec4(0, 0, 0, 1), color - vec4(.2, .2, .2, 0), 1. + normalized_pos.y * .5);
    float noise_color_mix = voronoi_value * .3 + perlin_value * .3 + fractal_value * .5;
    noise_color_mix = noise_color_mix * noise_color_mix;
    vec4 noise_color = mix(base_noise_color, vec4(0, 0, 0, 1.), noise_color_mix);

    float inner_alpha = smoothstep(.75 * link_anim, .85 * link_anim, edge_dist);
    if (link_anim < .1) inner_alpha = 1. - link_anim; // it's pixelated; this is fine

    vec4 base_edge_color = mix(vec4(0, 0, 0, 1), color, 1.5 + normalized_pos.y * .5);
    float edge_color_mix = 0.7 + voronoi_value * fractal_value * .5;
    vec4 edge_color = mix(vec4(0, 0, 0, 1), base_edge_color, edge_color_mix);

    float edge_mix = smoothstep(.65 + fractal_value * .1, .85 + voronoi_value * .1, edge_dist);

    float outer_mix = smoothstep(.9, 1., edge_dist);

    return vec4(mix(noise_color, edge_color, edge_mix).rgb, (1. - outer_mix) * inner_alpha);
}

void main() {
    float fractal_value = fractal_noise(vec3(tex_coord * 10., time / 50.));
    vec2 coord = floor(tex_coord * 16. + vec2(.5)) / 16.;
    vec4 color_at_coord = color_at(coord * (2. - open_anim), fractal_value);
    gl_FragColor = mix(vec4(0, 0, 0, color_at_coord.a), color_at_coord, 1. - fractal_value * .2);
}
