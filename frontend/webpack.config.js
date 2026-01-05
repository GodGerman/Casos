const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = {
  // Configuracion de desarrollo para construir el frontend localmente.
  mode: "development",
  // Entrada principal del bundle.
  entry: "./index.js",
  output: {
    filename: "main.js",
    path: path.resolve(__dirname, 'dist'),
    // Limpia dist/ en cada build para evitar archivos obsoletos.
    clean: true
  },
  resolve: {
    extensions: ['.js', '.jsx']
  },
  plugins: [
    new HtmlWebpackPlugin({
      // Inyecta el bundle dentro de la plantilla HTML.
      template: './public/index.html', // Ruta plantilla HTML
      filename: 'index.html', // Nombre del archivo de salida
      favicon: './img/icono.png'
    })
  ],
  module: {
    rules: [
      {
        // Transpila JS/JSX con Babel.
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: {
          loader: "babel-loader"
        }
      },
      {
        // Inyecta CSS en el bundle.
        test: /\.css$/,
        use: ["style-loader", "css-loader"]
      },
      {
        // Copia assets al directorio de salida.
        test: /\.(png|svg|jpg|jpeg|gif)$/i,
        type: 'asset/resource',
      }
    ]
  },
  devServer: {
    static: {
      directory: path.join(__dirname, 'dist'),
    },
    port: 8080, // Puerto del servidor
    open: true, // Abrir navegador automaticamente
    hot: true, // Habilitar Hot Module Replacement (HMR)
    historyApiFallback: true, // Aplicaciones SPA
  }
}
