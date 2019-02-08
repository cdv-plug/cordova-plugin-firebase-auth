var path = require('path');
var fs = require('fs');
var SEARCHED_POSTFIX = '.xcodeproj';

var plist = require('plist');

module.exports = function(context) {
  var isIosAdded = context.opts.platforms.includes('ios');

  if (!isIosAdded) {
    return;
  }

  var platformPath = path.join(context.opts.projectRoot, 'platforms', 'ios');
  var projectPath = fs.readdirSync(platformPath)
    .map(function(name) { return path.join(platformPath, name) })
    .filter(function (_path) { return !fs.statSync(_path).isFile()})
    .filter(function (_path) { return _path.indexOf(SEARCHED_POSTFIX) === (_path.length - SEARCHED_POSTFIX.length); })
    .pop()
    .replace(SEARCHED_POSTFIX, '')
  ;
  var projectName = projectPath.split(path.sep).pop();
  var projectPlistPath = path.join(projectPath, projectName + '-Info.plist');
  var gServicesPlistPath = path.join(projectPath, 'Resources', 'GoogleService-Info.plist');
  var list = plist.parse(fs.readFileSync(gServicesPlistPath, 'utf8'));
  var REVERSED_CLIENT_ID = list['REVERSED_CLIENT_ID'];
  var project = plist.parse(fs.readFileSync(projectPlistPath, 'utf-8'));

  if (!('CFBundleURLTypes' in project)) {
    project.CFBundleURLTypes = [];
  }

  project.CFBundleURLTypes.push({
    CFBundleTypeRole: 'Editor',
    CFBundleURLSchemes: [REVERSED_CLIENT_ID]
  });

  var newProjectPlistContent = plist.build(project);

  fs.writeFileSync(projectPlistPath, newProjectPlistContent);
  console.log('-- Patched ios project for google url scheme');
};