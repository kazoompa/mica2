angular.module('sfLocalizedString', [
  'schemaForm',
  'templates'
]).config(['schemaFormProvider', 'schemaFormDecoratorsProvider', 'sfBuilderProvider', 'sfPathProvider',
  function(schemaFormProvider,  schemaFormDecoratorsProvider, sfBuilderProvider, sfPathProvider) {

  var locStr = function(name, schema, options) {
    if (schema.type === 'object' && (schema.format == 'localizedString' || schema.format == 'localizedTextArea')) {
      var f = schemaFormProvider.stdFormObj(name, schema, options);
      f.key  = options.path;
      f.type = 'localizedstring';
      if(!f.locales) {
        f.locales = ['en'];
      }
      // f.validationMessage = {
      //   completed: 'All localized fields must be completed'
      // };
      // f.$validators = {
      //   completed: function(value) {
      //     if (value && Object.keys(value).length !== 0) {
      //       var count = f.locales.map(function(locale) {
      //         return value.hasOwnProperty(locale) ? 1 : 0;
      //       }).reduce(function (previous, current) {
      //         return previous + current;
      //       });
      //       console.log(f.locales.length === count);
      //       return f.locales.length === count;
      //     }
      //     return true;
      //   }
      // };
      options.lookup[sfPathProvider.stringify(options.path)] = f;
      return f;
    }
  };

  schemaFormProvider.defaults.object.unshift(locStr);
  
  schemaFormDecoratorsProvider.defineAddOn(
    'bootstrapDecorator',           // Name of the decorator you want to add to.
    'localizedstring',                      // Form type that should render this add-on
    'src/templates/sf-localized-string.html',  // Template name in $templateCache
    sfBuilderProvider.stdBuilders   // List of builder functions to apply.
  );

}]);