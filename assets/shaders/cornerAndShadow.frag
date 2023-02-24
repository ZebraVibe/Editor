// notes:
// variables are not always casted autom. so, when using floats make sure to add "."
// variables must affect output of the shader else they're being removed
// glFragColor = vec4(1,0,0,1) // ERROR will not work always
// some functions: sin(), cos(), tan(), asin(), acos(), atan(), pow(), exp(), log(),
// sqrt(), abs(), sign(), floor(), ceil(), fract(), mod(), min(), max() and clamp().
// step() smoothStep interpolate between 0-1
// mix() to mis colors
#ifdef GL_ES
// the precion of each variable - lowp, mediump, highp
// the more precision the more computing quality power required
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords; // from 0-1, (0.0) topleft corner of tex, (1.1) bottomRight corner of tex


uniform sampler2D u_texture;

uniform float u_radius; // tex radius + shadowSize
uniform float u_width, u_height;
uniform float u_x, u_y;
uniform float u_shadowAlpha;
uniform float u_shadowSize;

float calcCornerAlpha(in float distance){
    float texRadius = u_radius - u_shadowSize;
    float d = clamp(distance, texRadius, u_radius) - texRadius;
    return 1.0 - d / u_shadowSize;
}

void main() {
    vec2 pixelCoords = gl_FragCoord.xy - vec2(u_x,u_y);
    float normalX = pixelCoords.x / u_width;
    float normalY = pixelCoords.y / u_height;

    //vec2 pixelCoords = v_texCoords * vec2(width, height);
    float xMin = u_radius;
    float yMin = u_radius;
    float xMax = u_width - u_radius;
    float yMax = u_height - u_radius;

    float alpha = 1.0 ;
    vec4 color = v_color * texture2D(u_texture, v_texCoords);

    if(u_shadowSize > 0.0){
        // left
        if(pixelCoords.x < xMin){
            // top
            if(pixelCoords.y < yMin){
                float d = distance(pixelCoords, vec2(xMin, yMin));
                if(d > u_radius)discard;
                //alpha = 1 - (distance(pixelCoords, vec2(xMin, yMin)) / u_radius);
                alpha = calcCornerAlpha(d);

            //bottom
            }else if(pixelCoords.y > yMax){
                float d = distance(pixelCoords, vec2(xMin, yMax));
                if(d > u_radius)discard;
                //alpha = 1 - (distance(pixelCoords, vec2(xMin, yMax)) / u_radius);
                alpha = calcCornerAlpha(d);

            // left center
            } else {
                alpha = pixelCoords.x / u_shadowSize;
            }


        // right
        } else if(pixelCoords.x > xMax){
            // top
            if(pixelCoords.y < yMin){
                float d = distance(pixelCoords, vec2(xMax, yMin));
                if(d > u_radius)discard;
                //alpha = 1 - (distance(pixelCoords, vec2(xMax, yMin)) / u_radius);
                alpha = calcCornerAlpha(d);

            // bottom
            }else if(pixelCoords.y > yMax){
                float d = distance(pixelCoords, vec2(xMax, yMax));
                if(d > u_radius)discard;
                //alpha = 1 - (distance(pixelCoords, vec2(xMax, yMax)) / u_radius);
                alpha = calcCornerAlpha(d);

                // right center
            } else {
                alpha = 1 - ((pixelCoords.x - (u_width - u_shadowSize)) / u_shadowSize);
            }

        // middle
        } else {
            // top
            if(pixelCoords.y < yMin){
                alpha = pixelCoords.y / u_shadowSize;

             // bottom
            }else if(pixelCoords.y > yMax){
                alpha = 1 - ((pixelCoords.y - (u_height - u_shadowSize)) / u_shadowSize);

            }
        }

    }

    color *= vec4(vec3(0.0), alpha * u_shadowAlpha);
    gl_FragColor = color;

}

