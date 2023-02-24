
#ifdef GL_ES
// the precion of each variable - lowp, mediump, highp
// the more precision the more computing quality power required
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords; // from 0-1, (0.0) topleft corner of tex, (1.1) bottomRight corner of tex

uniform sampler2D u_texture;

uniform float u_x,u_y; // topLeft corner
uniform float u_radius;
uniform float u_width, u_height;

void main() {
    vec2 pixelCoords = gl_FragCoord.xy - vec2(u_x,u_y);
    float normalX = pixelCoords.x / u_width;
    float normalY = pixelCoords.y / u_height;

    //vec2 pixelCoords = v_texCoords * vec2(width, height);
    float xMin = u_radius;
    float yMin = u_radius;
    float xMax = u_width - u_radius;
    float yMax = u_height - u_radius;

    if(u_radius > 0.0){
        // left
        if(pixelCoords.x < xMin){
            // top
            if(pixelCoords.y < yMin
                && distance(pixelCoords, vec2(xMin, yMin)) > u_radius){
                discard;
            }
            // bottom
            if(pixelCoords.y > yMax
                && distance(pixelCoords, vec2(xMin, yMax)) > u_radius){
                discard;
            }
        // right
        } else if(pixelCoords.x > xMax){
            // top
            if(pixelCoords.y < yMin
            && distance(pixelCoords, vec2(xMax, yMin)) > u_radius){
                discard;
            }
            // bottom
            if(pixelCoords.y > yMax
            && distance(pixelCoords, vec2(xMax, yMax)) > u_radius){
                discard;
            }
        }

    }

    vec4 color = v_color * texture2D(u_texture, v_texCoords);
    //if(v_texCoords.x < 0.3)color *= vec4(0,0,0,1);
    //if(pixelCoords.x < 10)color *= vec4(1,0,0,1);
    //if(pixelCoords.y < 0)color *= vec4(0,1,0,1);

    //if(normalY < 0.5)color *= vec4(1,0,0,1);
    //if(normalY < 0)color *= vec4(0,1,0,1);


    //color *= vec4(v_texCoords.y, 0.0, 0.0, 1.0);
    gl_FragColor = color;
}

