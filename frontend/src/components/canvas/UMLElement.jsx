import React from 'react';

const SHAPES = {
    ACTOR: ({ width, height }) => (
        <g transform={`translate(${width / 2}, ${height / 2})`}>
            {/* Stick Figure centered */}
            <circle cx="0" cy="-25" r="8" fill="var(--bg-dark-800)" stroke="currentColor" strokeWidth="2" />
            <line x1="0" y1="-17" x2="0" y2="10" stroke="currentColor" strokeWidth="2" />
            <line x1="-15" y1="-10" x2="15" y2="-10" stroke="currentColor" strokeWidth="2" />
            <line x1="0" y1="10" x2="-12" y2="30" stroke="currentColor" strokeWidth="2" />
            <line x1="0" y1="10" x2="12" y2="30" stroke="currentColor" strokeWidth="2" />
        </g>
    ),
    CASO_DE_USO: ({ width, height }) => (
        <ellipse
            cx={width / 2}
            cy={height / 2}
            rx={width / 2 - 2}
            ry={height / 2 - 2}
            fill="var(--bg-dark-800)"
            stroke="currentColor"
            strokeWidth="2"
        />
    ),
    LIMITE_SISTEMA: ({ width, height }) => (
        <rect
            x="2"
            y="2"
            width={width - 4}
            height={height - 4}
            rx="0"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
        />
    ),
    PAQUETE: ({ width, height }) => (
        <g>
            <path
                d={`M2 10 h${width / 3} l4 -6 h${width / 3} v6 h${width / 3 - 6 - width / 3} v${height - 12} h-${width - 4} z`} // Simplified path logic just for concept, better to use standard shapes
                fill="none"
                stroke="none"
            />
            {/* Tab */}
            <path
                d={`M2 2 h${width / 2.5} v8 h-${width / 2.5} z`}
                fill="var(--bg-dark-800)"
                stroke="currentColor"
                strokeWidth="2"
            />
            {/* Body */}
            <rect
                x="2"
                y="10"
                width={width - 4}
                height={height - 12}
                fill="var(--bg-dark-800)"
                stroke="currentColor"
                strokeWidth="2"
            />
        </g>
    ),
    NOTA: ({ width, height }) => (
        <g>
            <path
                d={`M2 2 h${width - 16} l12 12 v${height - 16} h-${width - 4} z`}
                fill="var(--bg-dark-800)"
                stroke="currentColor"
                strokeWidth="2"
            />
            {/* Fold */}
            <path
                d={`M${width - 14} 2 v12 h12`}
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
            />
        </g>
    ),
    TEXTO: () => null, // Text handled by label outside SVG
    IMAGEN: ({ width, height }) => (
        <rect x="2" y="2" width={width - 4} height={height - 4} fill="var(--bg-dark-800)" stroke="currentColor" strokeWidth="2" strokeDasharray="4 4" />
    )
};

export default function UMLElement({ element, isSelected, width, height }) {
    const Shape = SHAPES[element.tipo_elemento];

    // Colors based on element type or selection could go here
    const strokeColor = isSelected ? 'var(--primary-400)' : 'var(--text-primary)';

    if (!Shape) {
        // Fallback for unknown types -> connection points or generic box
        return (
            <rect
                x="2"
                y="2"
                width={width - 4}
                height={height - 4}
                rx="4"
                fill="var(--bg-dark-800)"
                stroke={strokeColor}
                strokeWidth="2"
            />
        );
    }

    return (
        <svg width={width} height={height} className="overflow-visible">
            <g color={strokeColor}>
                <Shape width={width} height={height} />
            </g>
        </svg>
    );
}
