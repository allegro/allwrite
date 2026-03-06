# {{jreleaserCreationStamp}}
{{#brewRequireRelative}}
require_relative "{{.}}"
{{/brewRequireRelative}}

class {{brewFormulaName}} < Formula
  desc "{{projectDescription}}"
  homepage "{{projectLinkHomepage}}"
  version "{{projectVersion}}"
  license "{{projectLicense}}"

  {{brewMultiPlatform}}

  {{#brewHasLivecheck}}
  livecheck do
    {{#brewLivecheck}}
    {{.}}
    {{/brewLivecheck}}
  end
  {{/brewHasLivecheck}}
  {{#brewDependencies}}
  depends_on {{.}}
  {{/brewDependencies}}

  def install
    libexec.install Dir["*"]
    bin.install_symlink "#{libexec}/bin/{{distributionExecutableUnix}}" => "{{distributionExecutableName}}"


    # auto completions
    system "_ALLWRITE_COMPLETE=bash #{libexec}/bin/{{distributionExecutableUnix}} > bash_completion.sh"
    system "_ALLWRITE_COMPLETE=zsh #{libexec}/bin/{{distributionExecutableUnix}} > zsh_completion.sh"
    system "_ALLWRITE_COMPLETE=fish #{libexec}/bin/{{distributionExecutableUnix}} > fish_completion.sh"
    bash_completion.install "bash_completion.sh" => "{{distributionExecutableUnix}}"
    zsh_completion.install "zsh_completion.sh" => "_{{distributionExecutableUnix}}"
    fish_completion.install "fish_completion.sh" => "{{distributionExecutableUnix}}.fish"
  end

  def post_install
    if OS.mac?
      Dir["#{libexec}/lib/**/*.dylib"].each do |dylib|
        chmod 0664, dylib
        MachO::Tools.change_dylib_id(dylib, "@rpath/#{File.basename(dylib)}")
        MachO.codesign!(dylib)
        chmod 0444, dylib
      end
    end
  end

  test do
    output = shell_output("#{bin}/{{distributionExecutableName}} --version")
    assert_match "{{projectVersion}}", output
  end
end