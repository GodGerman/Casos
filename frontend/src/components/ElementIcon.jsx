import React from 'react';

// SVGs simplificados por tipo de elemento (para paleta y listados).
// Mantenerlos livianos ayuda a renderizar listas largas sin costo alto.
const ICONS = {
  ACTOR: (
    <>
      <circle cx="12" cy="6" r="3.5" />
      <path d="M12 9.5v7" />
      <path d="M8 12.5h8" />
      <path d="M12 16.5l-3 4" />
      <path d="M12 16.5l3 4" />
    </>
  ),
  CASO_DE_USO: (
    <ellipse cx="12" cy="12" rx="10" ry="6" />
  ),
  LIMITE_SISTEMA: (
    <rect x="4" y="4" width="16" height="16" rx="2" ry="2" strokeDasharray="4 3" />
  ),
  PAQUETE: (
    <>
      <path d="M4 8h7l2-2h7v14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8z" />
      <path d="M4 8h16" />
    </>
  ),
  NOTA: (
    <>
      <path d="M4 4h11l5 5v11a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z" />
      <path d="M15 4v5h5" />
    </>
  ),
  TEXTO: (
    <>
      <path d="M4 7V5h16v2" />
      <path d="M12 5v14" />
      <path d="M8 19h8" />
    </>
  ),
  IMAGEN: (
    <>
      <rect x="3" y="4" width="18" height="16" rx="2" />
      <circle cx="8.5" cy="8.5" r="1.5" />
      <path d="M21 15l-5-5L5 21" />
    </>
  ),
  RELACION: (
    <>
      <circle cx="6" cy="12" r="2" fill="currentColor" />
      <circle cx="18" cy="12" r="2" fill="currentColor" />
      <line x1="8" y1="12" x2="16" y2="12" />
    </>
  )
};

/**
 * Renderiza un icono SVG segun tipo de elemento.
 *
 * Se selecciona el SVG por tipo y lo envuelve en un
 * <svg> con tamaño configurable y atributos accesibles.
 *
 *
 * @param {{ type: string, size?: number, className?: string }} props props del icono.
 * @returns {JSX.Element|null} SVG o null si el tipo no existe.
 */
export default function ElementIcon({ type, size = 24, className = '' }) {
  const icon = ICONS[type];
  if (!icon) {
    return null;
  }
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={`element-icon-svg ${className}`}
      aria-hidden="true"
      focusable="false"
    >
      {icon}
    </svg>
  );
}
